(ns cljs-node-io.file "a bunch of nonsense for mocking java.io.File's polymorphic constructor"
  (:import goog.Uri)
  (:require [cljs.nodejs :as nodejs :refer [require]]
            [cljs-node-io.streams :refer [FileInputStream FileOutputStream]]
            [cljs-node-io.protocols
              :refer [IGetType get-type
                      Coercions as-url as-file
                      IOFactory make-reader make-writer make-input-stream make-output-stream]]))

(def fs (require "fs"))
(def path (require "path"))

(extend-type Uri
  IGetType
  (get-type [u] :uri))

(extend-type string
  IGetType
  (get-type [u] :string))

(defn ^Boolean append? [opts] (boolean (:append opts)))

(defn File*-dispatch
  ([o x] (get-type x)) ; string || Uri.
  ([o x y] ; [string string]  || [File string]
   (case (try (mapv get-type [x y]) (catch js/Object e (.log js/console e))) ;pre post instead?
     [:string :string] :string-string
     [:file :string] :file-string
     "default")))

(defmulti  File* "signature->File" File*-dispatch)

(defmethod File* :uri [o u]
  (let [pathstring (.getPath u)]
    (specify! o
      Object
      (to-url [f] (Uri. pathstring))
      (getPath [f] pathstring)
      (isAbsolute [_] (.isAbsolute path pathstring)))))

(defmethod File* :string  [o pathstring]
  (specify! o
    Object
    (to-url [f] (Uri. pathstring))
    (getPath [f] pathstring)
    (isAbsolute [_] (.isAbsolute path pathstring))))

(defmethod File* :string-string [o parent-str child-str]
  (let [pathstring (str parent-str "/" child-str)]
    (specify! o
      Object
      (to-url [f] (Uri. pathstring))
      (getPath [_] pathstring)
      (isAbsolute [_] (.isAbsolute path pathstring)))))


(defmethod File* :file-string [o parent-file child-str]
  (let [pathstring (str (.getPath parent-file) "/" child-str)]
    (specify! o
      Object
      (to-url [_] (Uri. pathstring))
      (getPath [_] pathstring)
      (isAbsolute [_] (.isAbsolute path pathstring)))))



(defn file-default-obj []
  (reify
    IEquiv
    (-equiv [this other] (= (.getPath this) (.getPath other))) ;is this the best way?
    IGetType
    (get-type [f] :file)
    Coercions
    (as-file [f] f)
    (as-url [f] (.to-url f))
    IOFactory
    (make-reader [this opts] (make-reader (make-input-stream this opts) opts))
    (make-writer [this opts] (make-writer (make-output-stream this opts) opts))
    (make-input-stream [^File file opts] (FileInputStream. file ))
    (make-output-stream [^File x opts] (FileOutputStream. x (append? opts)) opts)))



;java.io.File API

(defn File
  ([a] (File* (file-default-obj)  a))
  ([a b] (File* (file-default-obj) a b)))
