(ns cljs-node-io.test.async
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.test :refer-macros [deftest is async testing]]
            [cljs.core.async :as casync :refer [<! >! put! take! close! chan ]]
            [cljs-node-io.async :refer [go-proc event-onto-ch readable-onto-ch
                                        writable-onto-ch cp->ch sock->ch server->ch]]))

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

(defn emit [this evkw val]
  (.apply (.-emit this) this (into-array (cons (name evkw) val))))

(defn mock-stream [xf]
  (.setEncoding (new stream.Transform #js {"writableObjectMode" true "transform" xf}) "utf8"))

(defn readable-test-xf
  "[:end nil] will kill, emit 'end'
   [:data [v]] will buffer data, emit 'data'
   [:close [v]] will kill, emit 'end' & 'close'"
  [[ev val] encoding cb]
  (this-as this
    (case ev
      :data  (.push this (val 0))
      :end   (.push this nil) ;end of resource, kill stream
      :close (do (.push this nil) (emit this ev val))
      (emit this ev val))
    (cb)))

(def mock-readable-inputs
  [[:data ["a string"]]
   [:error [(js/Error. "some error")]]])

(defn writable-test-xf
  "strings are buffered, use to induce backpressure
   [:close [:a :b]] will trigger 'end' & 'close' events"
  [data encoding cb]
  (this-as this
    (if (string? data)
      (.push this data)
      (if (vector? data)
        (let [[ev val] data]
          (if (= ev :close) (.end this))
          (emit this ev val))))
    (cb)))

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
      (done))))

(deftest readable-onto-ch-test
  (async done
    (go
      (testing "readable-onto-ch :: 'error' , 'data', 'end' "
        (let [ts (mock-stream readable-test-xf)
              out (readable-onto-ch ts (chan) ["foo"])] ;"close"
          (doseq [[ev val :as msg] mock-readable-inputs]
            (.write ts msg)
            (is (= (<! out) msg)))
          (.write ts [:foo [42]])
          (is (= (<! out) [:foo [42]]))
          (.write ts [:end]) ;end method is writable mechanic, here resource is done
          (is (= (<! out) [:end nil]))
          (is (nil? (<! out)) "after 'end' event, chan should close")))
      (testing "readable-onto-ch: 'close' event semantics"
        (let [ts (mock-stream readable-test-xf)
              out (readable-onto-ch ts (chan) ["close"])
              close [:close [false]]
              exit-set (atom #{ close [:end nil]})]
          (.write ts close)
          (dotimes [_ (count @exit-set)] 
            (let [exit (<! out)
                  m? (if (@exit-set exit) (swap! exit-set disj exit))]
              (is (some? m?))))
          (is (empty? @exit-set))
          (is (nil? (<! out)))))
     (done))))

(deftest writable-onto-ch-test
  (async done 
    (go
      (testing "writable-onto-ch :: 'error' , 'drain', 'finish' "
        (let [ts (mock-stream writable-test-xf)
              out (writable-onto-ch ts (chan))
              e [:error [(js/Error. "some error")]]
              chunk (str (take 512 (repeat "C")))]
          (.write ts e)
          (is (= e (<! out)))
          (loop [] ;induce backpressure
            (if (.write ts chunk)
              (recur)))
          (.read ts)
          (is (= [:drain nil] (<! out))) ;signal to write again
          (.end ts)
          (is [:finish nil] (<! out))
          (is (nil? (<! out)))))
      (testing "writable-onto-ch: 'close' event semantics"
        (let [ts (mock-stream writable-test-xf)
              out (writable-onto-ch ts (chan) ["close"])
              close [:close [false]]
              exit-set (atom #{ close [:finish nil]})]
          (.write ts close)
          (dotimes [_ (count @exit-set)] 
            (let [exit (<! out)
                  m? (if (@exit-set exit) (swap! exit-set disj exit))]
              (is (some? m?))))
          (is (empty? @exit-set))
          (is (nil? (<! out)))))
      (done))))

(defn mock-proc []
  (let [o (EventEmitter.)
        stderr (mock-stream readable-test-xf)
        stdout (mock-stream readable-test-xf)
        stdin  (mock-stream writable-test-xf)
        write (fn [[k val]]
                (case k
                  :stderr (.write stderr val)
                  :stdout (.write stdout val)
                  :stdin (.write stdin val)
                  (emit o k val)))]
    (set! o.stdout stdout)
    (set! o.stderr stderr)
    (set! o.stdin stdin)
    (set! o.send true)
    (set! o.write write)
    o))

(def mock-stdio-inputs
  (concat
   (mapv #(conj [:stderr] %) mock-readable-inputs)
   (mapv #(conj [:stdout] %) mock-readable-inputs)))

(def proc-exits
  #{[:exit [:code :signal]]
    [:close [:code :signal]]
    [:stdin  [:close [false]]] [:stdin  [:finish nil]]
    [:stdout [:close [false]]] [:stdout [:end nil]]
    [:stderr [:close [false]]] [:stderr [:end nil]]})

(deftest cp->ch-test
  (async done
    (go
      (testing "cp->-ch"
        (let [p (mock-proc)
              out (cp->ch p)
              e [:error [(js/Error. "some error")]]]
          (testing "stdout, stderr :: 'error' & 'data'"
            (doseq [msg mock-stdio-inputs] 
              (.write p msg)
              (is (= msg (<! out)))))
          (testing "proc, stdin :: 'error'"
            (.write p e)
            (is (= e (<! out)))
            (let [msg (conj [:stdin] e)]
              (.write p msg)
              (is (= msg (<! out)))))
          (testing "proc :: 'disconnect', 'message' "
            (doseq [msg [[:disconnect nil] [:message [:msg nil]]]]
              (.write p msg)
              (is (= msg (<! out)))))
          (testing "proc close semantics"
            (.write p [:stdout [:close [false]]]) ;kill stdout
            (.write p [:stderr [:close [false]]]) ;kill stderr
            (.write p [:exit [:code :signal]]) ;proc exit a
            (.write p [:close [:code :signal]]) ;proc exit b
            (.write p [:stdin [:close [false]]]) ; kill stdin
            (let [exit-set (atom proc-exits)]
              (dotimes [_ (count @exit-set)]
                (let [exit (<! out)
                      m? (if (@exit-set exit) (swap! exit-set disj exit))]
                  (is (some? m?))))
              (is (empty? @exit-set))
              (is (nil? (<! out)))))))
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
      (let [sock (mock-stream readable-test-xf)
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
      (let [server (mock-stream readable-test-xf)
            out (server->ch server)]
        (doseq [msg server-events]
          (.write server msg)
          (is (= msg (<! out)))))
      (done))))
