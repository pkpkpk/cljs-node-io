(ns cljs-node-io.runner
  (:require [cljs.nodejs :as nodejs]
            [cljs-node-io.async-tests]
            [cljs-node-io.core-tests]
            [cljs-node-io.file-tests]
            [cljs-node-io.fs :refer [fexists?]]
            [cljs-node-io.fs-tests]
            [cljs-node-io.spawn-tests]))

(nodejs/enable-util-print!)
(set! (.-stackTraceLimit js/Error) 40)

(defn -main [& args]
  ; {:port 1234  :localAddress "..." :host "...."}
  ; (some-> (get-opts args) clj->js wire/start-client)
  (if-not (fexists? "src/test/cljs_node_io/fork_test.js")
    (throw (js/Error. "tests must be run from project root!"))
    (cljs.test/run-tests
     'cljs-node-io.core-tests
     'cljs-node-io.fs-tests
     'cljs-node-io.file-tests
     'cljs-node-io.spawn-tests
     'cljs-node-io.async-tests)))

(set! *main-cli-fn* -main)
