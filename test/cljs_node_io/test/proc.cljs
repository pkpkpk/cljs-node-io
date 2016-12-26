(ns cljs-node-io.test.proc
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.test :refer [deftest is testing run-tests are use-fixtures async]]
            [cljs.core.async :as casync :refer [<! put! take! chan close!]]
            [cljs-node-io.proc :as proc]))

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

(deftest fork-CP-test ;uncaughtError in child is not tested
  (async done
   (go
    (let [p "test/cljs_node_io/test/fork_test.js"
          ps (reset! PS (proc/fork p nil nil))
          CP  (proc/child ps)
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
          (let [msg [:disconnect nil]]
            (is (:connected CP))
            (is (= [nil] (<! (send msg))))
            (is (= msg (js->clj (<! CP))))
            (is (not (:connected CP))))))
      (testing "proc shutdown"
        (is (.write (.-stdin ps) "exit"))
        (let [end-set (atom #{[:stdin [:close [false]]]
                              [:stdout [:close [false]]]
                              [:stderr [:close [false]]]
                              [:stdin [:finish nil]]
                              [:stderr [:end nil]]
                              [:stdout [:end nil]]
                              [:exit [0 nil]]
                              [:close [0 nil]]})]
          (dotimes [_ (count @end-set)] ;nondet ordering
            (let [end (<! CP)
                  t (if (@end-set end)(do (swap! end-set disj end) true))]
              (is t)))
          (is (= @end-set #{}))
          (is (nil? (<! CP)))))
      (done)))))

(use-fixtures :once {:after #(if @PS (.kill @PS))})