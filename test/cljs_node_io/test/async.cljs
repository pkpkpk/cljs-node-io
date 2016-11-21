(ns cljs-node-io.test.async
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.test :refer-macros [deftest is async testing]]
            [cljs.core.async :as casync :refer [<! >! put! take! close! chan ]]
            [cljs-node-io.async :refer [go-proc event-onto-ch readable-onto-ch
                                        cp->ch sock->ch server->ch]]))

(defn make-handler [out]
  (fn [[k v :as msg]]
    (cond
      (= k :handler-error) (throw v)
      (= k :data) (put! out [k (inc v)])
      :else (put! out msg))))

(defn exit? [[k v]]
  (cond
    (= k :exit-error)(throw v)
    (= k :exit) true
    :else false))

(deftest go-proc-test
  (async done
    (go
      (let [in (chan 1)
           out (chan 1)
           err (chan 1)
           exit-ch (chan)
           handler (make-handler out)
           err-cb (fn [e] (put! err e))
           exit-cb #(close! exit-ch)
           gproc (go-proc in handler err-cb exit-cb exit?)]
        (testing "basic data handling"
          (>! in [:data 41])
          (is (= [:data 42] (<! out))))
        (testing "handler error"
          (let [[_ e :as msg] [:handler-error (js/Error. "handler-error")]]
            (>! in msg)
            (is (= e (:error (<! err))))
            (>! in [:data 41])
            (is (= [:data 42] (<! out)) "data handling should continue after a handler error")))
        (testing "exit? error"
          (let [[_ e :as msg] [:exit-error (js/Error. "exit?-error")]]
            (>! in msg)
            (is (= e (:error (<! err))))
            (is (= msg (<! out)) "data handling should continue after a exit? error")))
        (testing "exit condition"
          (let [msg [:exit :foo]]
            (is (.-active gproc))
            (>! in msg)
            (is (= msg (<! out)) "whatever triggered exit should still get processed")
            (is (nil? (<! exit-ch)))
            (is (not (.-active gproc))))))
     (done))))


(def stream (js/require "stream"))
(def EventEmitter (.-EventEmitter (js/require "events")))

(defn emit [this ev val]
  (.apply (.-emit this) this (into-array (cons (name ev) val))))

(defn readable-test-xf
  "mock behavior range of basic stream.readable
    (.write stream [:data val]) ;-> readable 'data' event
    (.write stream [:error err]) ;-> readable 'error' event
    (.write stream [:close val]) ;-> readable 'close' event
    (.write stream [:end nil]) ;-> readable 'end' event"
  [[ev val] encoding cb]
  (this-as this
    (case ev
      :data  (.push this (first val))
      :end   (.push this nil) ;this will kill the stream
      (emit this ev val))
    (cb)))

(defn mock-readable
  "for testing convenience converts bufs to strings. in practice
   expect buffers where encoding isn't explicitly set"
  []
  (let [ts (new stream.Transform #js {"writableObjectMode" true
                                      "transform" readable-test-xf})]
    (.setEncoding ts "utf8")))

(def mock-readable-inputs
  [[:data ["a string"]]
   [:error [(js/Error. "some error")]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest events-test
  (async done
    (go
      (testing "event-onto-ch"
        (let [c (chan)
             Em (EventEmitter.)
             _  (event-onto-ch Em c "foo")]
          (.emit Em "foo") ; cb() -> [:event nil]
          (is (= [:foo nil] (<! c)))
          (.emit Em "foo" :bar) ; cb(a) -> [:event [a]]
          (is (= [:foo [:bar]] (<! c)))
          (.emit Em "foo" :bar :baz) ; cb(a,b) -> [:event [a b]]
          (is (= [:foo [:bar :baz]] (<! c)))))
      (testing "readable-onto-ch"
        (let [ts (mock-readable)
              out (readable-onto-ch ts (chan) ["foo"])]
          (doseq [[ev val :as msg] mock-readable-inputs]
            (.write ts msg)
            (is (= (<! out) msg)))
          (.write ts [:foo [42]])
          (is (= (<! out) [:foo [42]]))))
      (done))))

(defn mock-proc []
  (let [o (EventEmitter.)
        stderr (mock-readable)
        stdout (mock-readable)
        write (fn [[k val] _ cb]
                (case k
                  :stderr (.write stderr val)
                  :stdout (.write stdout val)
                  :stdin (this-as this (apply emit (cons this val)))
                  (emit o k val))
                (cb))
        stdin  (stream.Writable. #js {"write" write "objectMode" true})]
    (set! o.stdout stdout)
    (set! o.stderr stderr)
    (set! o.stdin stdin)
    (set! o.send true)
    (set! o.write (fn [msg] (.write stdin msg)))
    o))

(def mock-stdio-inputs
  (concat
   (mapv #(conj [:stderr] %) mock-readable-inputs)
   (mapv #(conj [:stdout] %) mock-readable-inputs)
   [[:stdin [:error [(js/Error. "some error")]]]
    [:stdin [:end nil]]]))

(def mock-proc-inputs
  [[:exit [:code :signal]]
   [:close [:code :signal]]
   [:error [(js/Error. "some error")]]])

(def mock-fork-inputs [[:disconnect nil] [:message [:msg nil]]])

(deftest proc-tests
  (async done
    (go
      (testing "cp->-ch"
        (let [p (mock-proc)
              out (cp->ch p)]
          (testing "stdio"
            (doseq [msg mock-stdio-inputs]
              (.write p msg)
              (is (= msg (<! out)))))
          (testing "generic proc events"
            (doseq [msg mock-proc-inputs]
              (.write p msg)
              (is (= msg (<! out)))))
          (testing "fork events"
            (doseq [msg mock-fork-inputs]
              (.write p msg)
              (is (= msg (<! out)))))))
      (done))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def sock-events
  [[:data ["some data"]]
   [:error [(js/Error. "some error")]]
   [:connect nil]
   [:close [(js/Error. "some error")]]
   [:drain nil]
   [:lookup [(js/Error. "some error") "address" "family" "host"]]
   [:timeout nil]
   [:end nil]])

(deftest test-sock->ch
  (async done
    (go
      (let [sock (mock-readable)
            out (sock->ch sock)]
        (doseq [msg sock-events]
          (.write sock msg)
          (is (= msg (<! out)))))
      (done))))

(def server-events
  [[:error [(js/Error. "some error")]]
   [:connection [{:foo :bar}]]
   [:close nil]
   [:listening nil]])

(deftest test-server->ch
  (async done
    (go
      (let [server (mock-readable)
            out (server->ch server)]
        (doseq [msg server-events]
          (.write server msg)
          (is (= msg (<! out)))))
      (done))))
