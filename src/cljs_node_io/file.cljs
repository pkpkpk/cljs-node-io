(ns cljs-node-io.file "a bunch of nonsense for mocking java.io.File's polymoprhic constructor"
  (:import goog.Uri)
  (:require [cljs.nodejs :as nodejs :refer [require]]
            [cljs-node-io.util :refer [IGetType get-type  Coercions as-url as-file]] ))


(def fs (require "fs"))
(def path (require "path"))

(extend-type Uri
  IGetType
  (get-type [u] :uri))

(extend-type string
  IGetType
  (get-type [u] :string))

(defn File*-dispatch
  ([x] (get-type x)) ; string || Uri.
  ([x y] ; [string string]  || [File string]
   (case (try (mapv get-type [x y]) (catch js/Object e (.log js/console e))) ;pre post instead?
     [:string :string] :string-string
     [:file :string] :file-string
     "default")))

(defmulti  File* "signature->File" File*-dispatch)

(defmethod File* :uri [u]
  (let [pathstring (.getPath u)]
    (reify
     IEquiv;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
     (-equiv [_ other] (= pathstring (.getPath other)));;;;;;;;;;;;;;;;;;;;;;;;;;;
     IGetType
     (get-type [f] :file)
     Coercions
     (as-file [f] f)
     (as-url [f] (.to-url f))
     Object
     (to-url [f] (Uri. pathstring))
     (getPath [f] pathstring)
     (isAbsolute [_] (.isAbsolute path pathstring)))))

(defmethod File* :string  [pathstring]
  (reify
   IEquiv;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   (-equiv [_ other] (= pathstring (.getPath other)));;;;;;;;;;;;;;;;;;;;;;;;;;;
   IGetType
   (get-type [f] :file)
   Coercions
   (as-file [f] f)
   (as-url [f] (.to-url f))
   Object
   (to-url [f] (Uri. pathstring))
   (getPath [f] pathstring)
   (isAbsolute [_] (.isAbsolute path pathstring))))

(defmethod File* :string-string [parent-str child-str]
  (let [pathstring (str parent-str "/" child-str)]
    (reify
     IEquiv;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
     (-equiv [_ other] (= pathstring (.getPath other)));;;;;;;;;;;;;;;;;;;;;;;;;;;
     IGetType
     (get-type [f] :file)
     Coercions
     (as-file [f] f)
     (as-url [f] (.to-url f))
     Object
     (to-url [f] (Uri. pathstring))
     (getPath [f] pathstring)
     (isAbsolute [_] (.isAbsolute path pathstring)))))


(defmethod File* :file-string [parent-file child-str]
  (let [pathstring (str (.getPath parent-file) "/" child-str)]
    (reify
     IEquiv;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
     (-equiv [_ other] (= pathstring (.getPath other)));;;;;;;;;;;;;;;;;;;;;;;;;;;
     IGetType
     (get-type [f] :file)
     Coercions
     (as-file [f] f)
     (as-url [f] (.to-url f))
     Object
     (to-url [f] (Uri. pathstring))
     (getPath [f] pathstring)
     (isAbsolute [_] (.isAbsolute path pathstring)))))


(defn File
  ([a] (File* a))
  ([a b] (File* a b))
  ([a b c] (File* a b c)))
