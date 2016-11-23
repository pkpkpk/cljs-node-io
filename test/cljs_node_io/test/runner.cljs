(ns ^:figwheel-always cljs-node-io.test.runner
  (:require [cljs.test :refer-macros [run-tests]]
            [cljs-node-io.fs :refer [fexists?]]
            [cljs.nodejs :as nodejs]
            [cljs-node-io.test.core]
            [cljs-node-io.test.fs]
            [cljs-node-io.test.file]
            [cljs-node-io.test.proc]
            [cljs-node-io.test.async]
            [clojure.string :as string]
            [cljs.reader :refer [read-string]]
            [wire-report.core :as wire]))

(nodejs/enable-util-print!)
(set! (.-stackTraceLimit js/Error) 40)

; (defn on-js-reload [](.emit js/process "fw-reload"))

(defonce test-setup
  (.on js/process "uncaughtException"
    (fn [e]
      (if-not (wire/connected?)
        (js/console.error e)
        (wire/send [:uncaughtException [(wire/err->map e)]])))))

(defn get-opts [args]
  (if-let [a (first args)]
    (if (and (string/starts-with? a "{") (string/ends-with? a "}"))
       (read-string a))))

(defn -main [& args] ; uid+gid, tmpdir, select files & tests
  (let [opts (get-opts args) ; {:port 1234  :localAddress "..." :host "...."}
        client (if opts (wire/start-client (clj->js opts)))]
    (if-not (fexists? "test/cljs_node_io/test/fork_test.js")
      (throw (js/Error. "tests must be run from project root!"))
      (if (wire/connected?)
        (run-tests {:reporter :wire}
         'cljs-node-io.test.fs
         'cljs-node-io.test.file
         'cljs-node-io.test.core
         'cljs-node-io.test.proc
         'cljs-node-io.test.async)
        (run-tests
         'cljs-node-io.test.core
         'cljs-node-io.test.proc
         'cljs-node-io.test.fs
         'cljs-node-io.test.file
         'cljs-node-io.test.async)))))

(set! *main-cli-fn* -main)
