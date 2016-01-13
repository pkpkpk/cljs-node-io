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

(defn filepath-dispatch
  ([x] (try (get-type x) (catch js/Object e :default)) ) ; string || Uri.
  ([x y] ; [string string]  || [File string]
   (case (try (mapv get-type [x y]) (catch js/Object e nil)) ;pre post instead?
     [:string :string] :string-string
     [:file :string] :file-string
     :default)))

(defmulti  filepath "signature->File" filepath-dispatch)
(defmethod filepath :uri [u] (.getPath u))
(defmethod filepath :string  [pathstring] pathstring)
(defmethod filepath :string-string [parent-str child-str] (str parent-str "/" child-str))
(defmethod filepath :file-string [parent-file child-str] (str (.getPath parent-file) "/" child-str))
(defmethod filepath :default [x] (throw (js/Error.
                                         (str "\nUnrecognized path configuration passed to File constructor."
                                              "\nYou passed " (pr-str x)
                                              "\nYou must pass a [string], [uri], [string string], or [file string].\n" ))))

(defn file-reader [f opts]
  (if (:stream? opts)
    (make-reader (make-input-stream f opts) opts)
    (.readFileSync fs (.getPath f) (or (:encoding opts) "utf8"))))

(defn File* [pathstring]
  (reify
    IEquiv
    (-equiv [this other] (= (.getPath this) (.getPath other))) ;is this the best way?
    IGetType
    (get-type [f] :file)
    Coercions
    (as-file [f] f)
    (as-url [f] (.to-url f))
    IOFactory
    (make-reader [this opts] (file-reader this opts))
    (make-writer [this opts] (make-writer (make-output-stream this opts) opts))
    (make-input-stream [^File file opts] (FileInputStream. file ))
    (make-output-stream [^File x opts] (FileOutputStream. x (append? opts)) opts)
    Object
    (delete [this] (.unlinkSync fs (.getPath this)))
    (deleteOnExit [this] (.on js/process "exit" #(.delete this)))
    (to-url [f] (Uri. pathstring))
    (getPath [f] pathstring)
    (isAbsolute [_] (.isAbsolute path pathstring))))

(defn File
  ([a] (File*  (filepath a)))
  ([a b] (File*  (filepath a b))))
