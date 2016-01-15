(ns ^:figwheel-always cljs-node-io.util
  ; (:require-macros [cljs-node-io.macros :refer [with-open]])
  (:require [cljs.nodejs :as nodejs :refer [require]]
            [cljs.reader :refer [read-string]]
            [cljs-node-io.core :refer [reader writer]])
  (:import [goog.string StringBuffer] )
  )

(def fs (require "fs"))
(def path (require "path"))




(defn slurp-stream??
  "Opens a reader on f and reads all its contents, returning a string.
  See clojure.java.io/reader for a complete list of supported arguments."
   [f]
   (let [sb  (StringBuffer.)
         r   (apply reader f nil)
         res (atom nil)] ; channel?
     (doto r
       (.on "error" (fn [e] (throw e)))
       (.on "readable"
          (fn []
            (loop [chunk (.read r 1)]
              (if (nil? chunk)
                (reset! res (.toString sb))
                (do
                  (.append sb chunk)
                  (recur (.read r)))))))) res))



(defn slurp
  "Opens a reader on f and reads all its contents, returning a string.
  See clojure.java.io/reader for a complete list of supported arguments."
  {:added "1.0"}
  ([f] (apply reader f nil))
  ([f & opts] ;should select on :stream? true in opts somehow
     ;needs to return a chan containing async results
     (let [r (apply reader f opts)
           sb (StringBuffer.)]
         (loop [c (.read r)]
           (if (neg? c)
             (.toString sb)
             (do
               (.append sb c)
               (recur (.read r))))))))




(defn sslurp
  "augmented 'super' slurp for convenience. edn|json => clj data-structures"
  [filepath]
  (let [contents (slurp filepath)]
    (condp = (.extname path filepath)
      ".edn"  (read-string contents)
      ".json" (js->clj (js/JSON.parse contents) :keywordize-keys true)
      ;xml, csv, disable-auto-reading?
      (throw (js/Error. "sslurp was given an unrecognized file format.
                         The file's extension must be json or edn")))))


(defn spit
  "Opposite of slurp.  Opens f with writer, writes content, then
  closes f. Options passed to clojure.java.io/writer."
  [f content & options]
  (let [w (apply writer f options)]
    (.write w (str content))))



; (defn line-seq
;   "Returns the lines of text from rdr as a lazy sequence of strings.
;   rdr must implement java.io.BufferedReader."
;   {:added "1.0"
;    :static true}
;   [^java.io.BufferedReader rdr]
;   (when-let [line (.readLine rdr)]
;     (cons line (lazy-seq (line-seq rdr)))))
;
; (defn file-seq
;   "A tree seq on java.io.Files"
;   {:added "1.0"
;    :static true}
;   [dir]
;     (tree-seq
;      (fn [^java.io.File f] (. f (isDirectory)))
;      (fn [^java.io.File d] (seq (. d (listFiles))))
;      dir))
