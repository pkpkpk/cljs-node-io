(ns cljs-node-io.util)


(defprotocol IGetType
  (get-type [this] "type helper for dispatch"))

(defprotocol Coercions
  "Coerce between various 'resource-namish' things."
  (^{:tag :File} as-file [x] "Coerce argument to a file.")
  (^{:tag :URL} as-url [x] "Coerce argument to a URL."))
