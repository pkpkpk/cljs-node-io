(ns cljs-node-io.test.file
  (:require [cljs.test :refer-macros [deftest is testing run-tests are]]
            [cljs-node-io.file :refer [File temp-file]]
            [cljs-node-io.protocols :refer [Coercions as-file as-url ]]
            [cljs-node-io.core :refer [spit slurp]]) ;file File
  (:import goog.Uri))
