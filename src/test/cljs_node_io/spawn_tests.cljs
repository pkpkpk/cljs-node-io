(ns cljs-node-io.spawn-tests
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.test :refer [deftest is testing run-tests are use-fixtures async]]
            [cljs.core.async :as casync :refer [<! put! take! chan close!]]
            [cljs-node-io.spawn :refer [fork cp->ch]]
            [cljs-node-io.test.async-helpers
             :refer [emit mock-stream readable-test-xf mock-readable-inputs
                     writable-test-xf]]))

(def EventEmitter (.-EventEmitter (js/require "events")))

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
          ; ps (reset! PS (proc/fork p nil nil))
          ; CP  (proc/child ps)
          CP (fork p nil nil)
          ps (.-proc CP)
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