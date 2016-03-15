(ns ^:figwheel-always cljs-node-io.test.runner
  (:require [cljs.test :refer-macros [run-tests run-all-tests]]
            [cljs-node-io.test.core]
            [cljs-node-io.test.file])
  (:import goog.Uri))


(run-all-tests #"^cljs-node-io\.test.*")
