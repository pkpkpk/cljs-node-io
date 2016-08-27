(ns ^:figwheel-always cljs-node-io.test.runner
  (:require [cljs.test :refer-macros [run-all-tests]]
            [cljs.nodejs :as nodejs]
            [cljs-node-io.test.core]
            [cljs-node-io.test.fs]
            [cljs-node-io.test.file]))

(nodejs/enable-util-print!)

(run-all-tests #"^cljs-node-io\.test.*")

(defn -main [& args] nil)
(set! *main-cli-fn* -main)