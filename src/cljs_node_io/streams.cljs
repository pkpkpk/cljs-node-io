(ns cljs-node-io.streams
  (:import goog.Uri)
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

(defn filepath-dispatch [f {:keys [fd]}]
  (if (or (and (integer? f)  (isFd? f)) ;redundant?
          (and (integer? fd) (isFd? fd)))
    :file-descriptor
    (type f)))


(defmulti filepath filepath-dispatch) ;should check path validity, URI too
(defmethod filepath :file-descriptor [fd _] fd)
(defmethod filepath js/String [pathstring _] pathstring)
(defmethod filepath Uri [u _] (.getPath u))
(defmethod filepath :File [file _] (.getPath file))
(defmethod filepath :default [p _] p)

(defn valid-opts [opts]
  (if opts
    (clj->js opts) ;...
    ""))




(defn attach-input-impls! [filestreamobj] ; <THIS IS _FILE_ STREAM specific
  (let [filedesc      (atom nil)
        _             (set! (.-constructor filestreamobj) :FileInputStream)
        _             (.on filestreamobj "open" (fn [fd] (reset! filedesc fd )))]
    (specify! filestreamobj
      IOFactory
      (make-reader [this opts] this)
      (make-input-stream [this _] this)
      (make-writer [this _] (throw (js/Error. (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str this) "> as an OutputStream."))))
      (make-output-stream [this _] (throw (js/Error. (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str this) "> as an OutputStream."))))
      Object
      (getFd [_] @filedesc))))

(defn FileInputStream* [src opts]
  (let [filestreamobj (.createReadStream fs (filepath src) (valid-opts opts))]
    (attach-input-impls! filestreamobj)))

(defn FileInputStream
  ([file] (FileInputStream file nil))
  ([file opts] (FileInputStream* file opts)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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


(defn FileOutputStream* [src opts]
  (let [filestreamobj (.createWriteStream fs (filepath src) (valid-opts opts))]
    (attach-output-impls! filestreamobj)))

(defn FileOutputStream
  ([file] (FileOutputStream file nil))
  ([file opts] (FileOutputStream* file opts)))


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
  [{:keys [transform flush] :as opts}]
  (assert (map? opts) "you must pass a map of constructor options containing at least :read & :write fns")
  (assert (fn? transform) "you must supply a :transform fn when creating a transform stream.")
  (assert (if flush (fn? flush) true) ":flush must be a fn")
  (new stream.Transform (clj->js opts)))
