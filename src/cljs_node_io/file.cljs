(ns cljs-node-io.file "a bunch of nonsense for mocking java.io.File's polymoprhic constructor"
  (:import goog.Uri)
  (:require [cljs.nodejs :as nodejs :refer [require]]
            [cljs-node-io.util :refer [append?
                                       IGetType get-type
                                       Coercions as-url as-file
                                       IOFactory make-reader make-writer make-input-stream make-output-stream]] ))


; try making a generic reified object that has all common methods
; and use specify! to tailor each object to specific types to reduce repetitive code

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
   IOFactory
   (make-reader [x opts] (make-reader (make-input-stream x opts) opts))
   (make-writer [x opts] (make-writer (make-output-stream x opts) opts))
   (make-input-stream [^File x opts] (make-input-stream (FileInputStream. x) opts))
   (make-output-stream [^File x opts] (make-output-stream (FileOutputStream. x (append? opts)) opts))
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



; (defn file-default-obj [])



(defn File
  ([a] (File* a))
  ([a b] (File* a b))
  ([a b c] (File* a b c)))



; :make-reader (fn [x opts] (make-reader (make-input-stream x opts) opts))
; :make-writer (fn [x opts] (make-writer (make-output-stream x opts) opts))
; :make-input-stream (fn [^File x opts] (make-input-stream (FileInputStream. x) opts))
; :make-output-stream (fn [^File x opts] (make-output-stream (FileOutputStream. x (append? opts)) opts)))
