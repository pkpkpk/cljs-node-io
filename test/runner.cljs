(ns ^:figwheel-always cljs-node-io.test.runner
  (:require [cljs.test :refer-macros [run-all-tests]]
            [cljs-node-io.test.core]
            [cljs-node-io.test.file]))

(run-all-tests #"^cljs-node-io\.test.*")