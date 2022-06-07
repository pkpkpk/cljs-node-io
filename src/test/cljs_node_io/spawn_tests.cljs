(ns cljs-node-io.spawn-tests
  (:require [cljs.test :refer [deftest is testing use-fixtures async]]
            [cljs.core.async :as casync :refer [go <! put! take! chan]]
            [cljs-node-io.spawn :refer [fork cp->ch]]))

(def EventEmitter (.-EventEmitter (js/require "events")))

(deftest cp->ch-test
  "This tests the spawn/cp->ch function's ability to attach listeners to
   ChildProcess events and forward them appropriately"
  (async done
    (go
     (let [p (let [p (EventEmitter.)]
               (set! (.-stdin p) (EventEmitter.))
               (set! (.-stdout p) (EventEmitter.))
               (set! (.-stderr p) (EventEmitter.))
               (set! (.-send p) true)
               p)
           out (cp->ch p)]
       (testing "stdio"
         (testing "stdout :: 'data' 'error' 'end' 'close'"
           (.emit (.-stdout p) "data" "some-data")
           (is (= [:stdout [:data ["some-data"]]] (<! out)))
           (.emit (.-stdout p) "error" ::an-error)
           (is (= [:stdout [:error [::an-error]]] (<! out)))
           (.emit (.-stdout p) "end")
           (is (= [:stdout [:end nil]] (<! out)))
           (.emit (.-stdout p) "close")
           (is (= [:stdout [:close nil]] (<! out))))
         (testing "stderr :: 'data' 'error' 'end' 'close'"
           (.emit (.-stderr p) "data" "some-data")
           (is (= [:stderr [:data ["some-data"]]] (<! out)))
           (.emit (.-stderr p) "error" ::an-error)
           (is (= [:stderr [:error [::an-error]]] (<! out)))
           (.emit (.-stderr p) "end")
           (is (= [:stderr [:end nil]] (<! out)))
           (.emit (.-stderr p) "close")
           (is (= [:stderr [:close nil]] (<! out))))
         (testing "stdin :: 'error' 'finish' 'close'"
           (.emit (.-stdin p) "error" ::an-error)
           (is (= [:stdin [:error [::an-error]]] (<! out)))
           (.emit (.-stdin p) "finish")
           (is (= [:stdin [:finish nil]] (<! out)))
           (.emit (.-stdin p) "close")
           (is (= [:stdin [:close nil]] (<! out)))))
       (testing "process :: 'error', 'message', 'disconnect', 'close', 'exit'"
         (.emit p "error" ::an-error)
         (is (= [:error [::an-error]] (<! out)))
         (.emit p "message" "this is a msg!")
         (is (= [:message ["this is a msg!" nil]] (<! out)))
         (.emit p "disconnect")
         (is (= [:disconnect] (<! out)))
         (.emit p "close" :code :signal)
         (is (= [:close [:code :signal]] (<! out)))
         (.emit p "exit" :code :signal)
         (is (= [:exit [:code :signal]] (<! out)))
         (testing "exit conditions have been met, chan shuts down"
           (nil? (<! out)))))
      (done))))

(defn intercept-send
  "serverside ChildProcess events & stream errors cannot be induced by client
   so we must simulate them"
  [ps CP]
  (fn [[k val :as msg]] ;this needs to be expanded for cb, handle+opts
    (if (or (= :error k) (= :error (first val)))
      (let [[_ [e]] val]
        (case k
          :error  (.emit ps "error" (first val))
          :stderr (.emit (.-stderr ps) "error" e)
          :stdout (.emit (.-stdout ps) "error" e)
          :stdin  (.emit (.-stdin ps) "error" e)))
      (.send CP msg))))

(def stdio-inputs
  [[:stderr [:error [(js/Error. "some error")]]]
   [:stdout [:error [(js/Error. "some error")]]]
   [:stdin  [:error [(js/Error. "some error")]]]
   [:stderr [:data ["this string should be written to stderr"]]]
   [:stdout [:data ["this string should be written to stderr"]]]])

(defonce PS (atom nil))

;; TODO uncaughtError in child is not tested
(deftest fork-CP-test
  (async done
   (go
    (let [p "src/test/cljs_node_io/fork_test.js"
          CP (fork p nil nil)
          ps (reset! PS (.-proc CP))
          send (intercept-send ps CP)]
      (is (:connected CP))
      (testing "testing ChildProcess ReadPort"
        (testing "stdio"
          (doseq [msg stdio-inputs]
            (send msg)
            (is (= msg (<! CP)))))
        (testing "proc error & IPC"
          (let [msg [:error [(js/Error "proc error")]]]
            (send msg)
            (is (= msg (<! CP))))
          (let [msg [:message ["hello world!" nil]]]
            (is (= [nil] (<! (send msg))))
            (is (= msg (js->clj (<! CP)))))
          (let [msg [:disconnect]]
            (is (:connected CP))
            (is (= [nil] (<! (send msg))))
            (is (= msg (js->clj (<! CP))))
            (is (not (:connected CP))))))
      (testing "proc shutdown"
        (is (.write (.-stdin ps) "exit"))
        (.end (.-stdin ps))
        (let [expected-end-events #{[:stdin [:finish nil]]
                                    [:stdin [:close [false]]]

                                    [:stdout [:close [false]]]
                                    [:stdout [:end nil]]

                                    [:stderr [:close [false]]]
                                    [:stderr [:end nil]]

                                    [:exit [42 nil]]
                                    [:close [42 nil]]}
              output (<! (cljs.core.async/into [] CP))]
          (is (= (set output) expected-end-events))
          (is (nil? (<! CP)))
          (done)))))))

(use-fixtures :once {:after #(if @PS (.kill @PS))})
