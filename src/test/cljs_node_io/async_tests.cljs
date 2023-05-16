(ns cljs-node-io.async-tests
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.test :refer-macros [deftest is async testing]]
            [cljs.core.async :as casync :refer [<! >! put! take! close! chan ]]
            [cljs-node-io.async :refer [go-proc mux admux unmux unmux-all
                                        event-onto-ch readable-onto-ch writable-onto-ch
                                        sock->ch server->ch]]
            [cljs-node-io.async-test-helpers
             :refer [emit mock-stream readable-test-xf mock-readable-inputs
                     writable-test-xf]]))

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
            (is (nil? (<! gproc)))
            (is (not (.-active gproc))))))
     (done))))

(deftest mux-test
  (async done
   (let [out (chan 1)
         in (chan 1)
         err (chan 1)
         e (js/Error. "some error")
         v :foo
         handler (fn [src n]
                   (if-not (number? n)
                     (throw e)
                     (put! out [src (inc n)])))
         eh (fn [m](put! err m))
         mx (mux handler eh)]
     (go
      (testing "admux"
        (is (empty? (.ports mx)))
        (admux mx in)
        (is (= #{in} (.ports mx)))
        (admux mx in)
        (is (= #{in} (.ports mx))))
      (testing "handling"
        (>! in 42)
        (is (= [in 43] (<! out))))
      (testing "error handling"
        (>! in v)
        (is (= (<! err) {:e e :v v :msg "mux: data handler error"})))
      (testing "removal of closed ports"
        (admux mx out)
        (is (= #{in out} (.ports mx)))
        (>! in 42)
        (close! in)
        (<! out) ;wait a round
        (is (= #{out} (.ports mx))))
      (testing "unmux"
        (unmux mx out)
        (is (empty? (.ports mx))))
      (testing "unmux-all"
        (admux mx in)
        (admux mx out)
        (admux mx err)
        (is (= #{in out err}) (.ports mx))
        (unmux-all mx)
        (is (empty? (.ports mx))))
      (testing "kill"
        (is (.-active mx))
        (admux mx in)
        (.kill mx)
        (is (nil? (<! mx)))
        (is (empty? (.ports mx)))
        (is (false? (admux mx in))))
      (done)))))

(def EventEmitter (.-EventEmitter (js/require "events")))

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
