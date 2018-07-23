(ns cljs-node-io.runner
  (:require [wire-report.core :as wire :include-macros true]
            [clojure.string :as string]
            [cljs.reader :refer [read-string]]
            [cljs-node-io.fs :refer [fexists?]]
            [cljs.nodejs :as nodejs]
            [cljs-node-io.core-tests]
            [cljs-node-io.fs-tests]
            [cljs-node-io.file-tests]
            [cljs-node-io.async-tests]
            [cljs-node-io.spawn-tests]))

(nodejs/enable-util-print!)
(set! (.-stackTraceLimit js/Error) 40)

(defn get-opts [args]
  (if-let [a (first args)]
    (if (and (string/starts-with? a "{") (string/ends-with? a "}"))
       (read-string a))))

(defn -main [& args] ; uid+gid, tmpdir, select files & tests
  (let [opts (get-opts args) ; {:port 1234  :localAddress "..." :host "...."}
        client (if opts (wire/start-client (clj->js opts)))]
    (if-not (fexists? "src/test/cljs_node_io/fork_test.js")
      (throw (js/Error. "tests must be run from project root!"))
      (wire/run-tests
        'cljs-node-io.core-tests
        'cljs-node-io.fs-tests
        'cljs-node-io.file-tests
        'cljs-node-io.spawn-tests
        'cljs-node-io.async-tests))))

(set! *main-cli-fn* -main)

