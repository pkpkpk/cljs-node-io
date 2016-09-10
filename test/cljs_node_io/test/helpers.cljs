(ns  cljs-node-io.test.helpers
  (:require [cljs-node-io.file :refer [File]]))

(def os (js/require "os"))
(def path (js/require "path"))


(defn createTempFile
  ([prefix] (createTempFile prefix nil nil))
  ([prefix suffix] (createTempFile prefix suffix nil))
  ([prefix suffix dir]
    (let [tmpd (or dir (.tmpdir os))
          path (str tmpd (.-sep path) prefix (or suffix ".tmp"))
          f    (File. path)]
      f)))
