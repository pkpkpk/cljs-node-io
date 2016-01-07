(ns  cljs-node-io.reader
  ; (:require-macros [cljs-node-io.macros :refer [with-open]])
  (:require [cljs.nodejs :as nodejs :refer [require]]
            ; [cljs-node-io.streams :refer [FileInputStream]]
            [cljs-node-io.util :refer [append?
                                       IGetType get-type
                                       Coercions as-url as-file
                                       IOFactory make-reader make-writer make-input-stream make-output-stream]]))



(defn reader [input-stream opts ] input-stream)
