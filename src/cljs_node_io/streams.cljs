(ns cljs-node-io.streams
  (:require [cljs.nodejs :as nodejs :refer [require]]
            [cljs.core.async :as async :refer [put! take! chan <! pipe  alts!]]
            [cljs-node-io.protocols
              :refer [Coercions as-url as-file
                      IOFactory make-reader make-writer make-input-stream make-output-stream]]))

(def fs (require "fs"))
(def stream (require "stream"))

(defn ^Boolean isFd? "is File-descriptor?"
  [path]
  (= path (unsigned-bit-shift-right path 0)))

(def default-input-options
  {
  ;  :flags "r" ;-> under the hood default
  ;  :encoding "utf8" ;-> under the hood default string literal
  ;  :mode "0o666" ;-> under the hood default
   :fd nil ;if specified, will ignore path argument, therefore no 'open' event
   :autoClose true})

(defn stream-reader [st opts] st) ; good place to setup async handlers?


(defn attach-input-impls! [filestreamobj] ; <THIS IS FILE STREAM specific
  (let [filedesc      (atom nil)
        _             (set! (.-constructor filestreamobj) :FileInputStream)
        _             (.on filestreamobj "open" (fn [fd] (reset! filedesc fd )))]
    (specify! filestreamobj
      IOFactory
      (make-reader [this opts] (stream-reader this opts))
      (make-input-stream [this _] this)
      (make-writer [this _] (throw (js/Error. (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str this) "> as an OutputStream."))))
      (make-output-stream [this _] (throw (js/Error. (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str this) "> as an OutputStream."))))
      Object
      (getFd [_] @filedesc))))



(defn file-stream-dispatch [f {:keys [fd]}]
  (if (or (and (integer? f)  (isFd? f)) ;redundant?
          (and (integer? fd) (isFd? fd)))
    :file-descriptor
    (type f)))


(defmulti filepath file-stream-dispatch)
; (defmethod filepath :file-descriptor [fd _] fd)
; (defmethod filepath js/String [pathstring _] pathstring)
; Uri?
(defmethod filepath :File [file _] (.getPath file))
(defmethod filepath :default [p _] p)



(defn FileInputStream* [src opts]
  (let [streamobj (.createReadStream fs (filepath src) (clj->js opts))
        _         (attach-input-impls! streamobj)]

    streamobj))


(defn FileInputStream ; file|string|file-descriptor => stream-object => Buffer
  ([file] (FileInputStream* file default-input-options))
  ([file opts] (FileInputStream* file (merge default-input-options opts))))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def default-output-options
  { :flags "w" ;r+ = append?
    :defaultEncoding "utf8" ; can be any encoding accepted by Buffer
    :fd nil ;if specified, will ignore path argument, therefore no 'open' event
    ; :start -> used to write at some postion past the beginning of the file
    ; :mode "0o666" ;needs to be parsed
   })

(defn attach-output-impls! [filestreamobj]
  (let [filedesc      (atom nil)
        _             (set! (.-constructor filestreamobj) :FileOutputStream)
        _             (.on filestreamobj "open" (fn [fd] (reset! filedesc fd )))]
    (specify! filestreamobj
      IOFactory
      (make-reader [this _] (throw (js/Error. (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str this) "> as an InputStream."))))
      (make-input-stream [this _] (throw (js/Error. (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str this) "> as an InputStream."))))
      (make-writer [this _] this)
      (make-output-stream [this _] this)
      Object
      (getFd [_] @filedesc))))


(defmulti FileOutputStream* (fn [f opts] (type f)))

(defmethod FileOutputStream* :string [pathstring opts] ;should never reach this via IOFactor, should be coerced to file or Uri
  (let [filestreamobj (.createWriteStream fs pathstring (clj->js opts)) ] ;should check path validity, URI too
    (attach-output-impls! filestreamobj)))

(defn FileOutputStream
  ([file] (FileOutputStream* file default-output-options))
  ([file opts] (FileOutputStream* file (merge default-output-options opts))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ReadableStream
  [{:keys [read] :as opts}]
  (assert (map? opts) "you must pass a map of constructor options containing at least a :read k-v pair")
  (assert (fn? read) "you must supply an internal :read function when creating a read stream")
  (new stream.Readable (clj->js opts)))

(defn WritableStream
  [{:keys [write] :as opts}]
  (assert (map? opts) "you must pass a map of constructor options containing at least a :write k-v pair")
  (assert (fn? write) "you must supply an internal :write function when creating writable streams")
  (new stream.Writable (clj->js opts)))

(defn DuplexStream
  [{:keys [read write] :as opts}]
  (assert (map? opts) "you must pass a map of constructor options containing at least :read & :write fns")
  (assert (and (fn? read) (fn? write)) "you must supply :read & :write fns when creating duplex streams.")
  (new stream.Duplex (clj->js opts)))

(defn TransformStream
  [{:keys [transform] :as opts}]
  (assert (map? opts) "you must pass a map of constructor options containing at least :read & :write fns")
  (assert (and (fn? transform)) "you must supply :read & :write fns when creating duplex streams.")
  (new stream.Transform (clj->js opts)))
