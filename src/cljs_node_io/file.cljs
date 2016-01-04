(ns cljs-node-io.file "a bunch of nonsense for mocking java.io.File's polymoprhic constructor"
  (:import goog.Uri)
  (:require [cljs.nodejs :as nodejs :refer [require]]
            [cljs-node-io.util :refer [append?
                                       IGetType get-type
                                       Coercions as-url as-file
                                       IOFactory make-reader make-writer make-input-stream make-output-stream]] ))



(deftype FileInputStream [x])
(deftype FileOutputStream [x y])




(def fs (require "fs"))
(def path (require "path"))

(extend-type Uri
  IGetType
  (get-type [u] :uri))

(extend-type string
  IGetType
  (get-type [u] :string))

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
    (make-reader [x opts] (make-reader (make-input-stream x opts) opts))
    (make-writer [x opts] (make-writer (make-output-stream x opts) opts))
    (make-input-stream [^File x opts] (make-input-stream (FileInputStream. x) opts))
    (make-output-stream [^File x opts] (make-output-stream (FileOutputStream. x (append? opts)) opts))))



(defn File
  ([a] (File* (file-default-obj)  a))
  ([a b] (File* (file-default-obj) a b))
  ([a b c] (File* (file-default-obj) a b c)))
