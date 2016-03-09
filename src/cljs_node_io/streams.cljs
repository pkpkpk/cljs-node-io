(ns cljs-node-io.streams
  (:import goog.Uri)
  (:require [cljs.nodejs :as nodejs :refer [require]]
            [cljs.core.async :as async :refer [put! take! chan <! pipe  alts!]]
            [cljs-node-io.protocols
              :refer [Coercions as-url as-file
                      IOFactory make-reader make-writer make-input-stream make-output-stream]]))

(def fs (require "fs"))
(def stream (require "stream"))

(defn input-IOF!
  "adds IOFactory input impls that just defer back to the stream or throw as appropriate"
  [streamobj]
  (specify! streamobj
    IOFactory
    (make-reader [this opts] this)
    (make-input-stream [this _] this)
    (make-writer [this _] (throw (js/Error. (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str this) "> as an OutputStream."))))
    (make-output-stream [this _] (throw (js/Error. (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str this) "> as an OutputStream."))))))

(defn output-IOF!
  "adds IOFactory output impls that just defer back to the stream or throw as appropriate"
  [streamobj]
  (specify! streamobj
    IOFactory
    (make-reader [this _] (throw (js/Error. (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str this) "> as an InputStream."))))
    (make-input-stream [this _] (throw (js/Error. (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str this) "> as an InputStream."))))
    (make-writer [this _] this)
    (make-output-stream [this _] this)))

(defn duplex-IOF!
  "defer back to the stream in all cases"
  [streamobj]
  (specify! streamobj
    IOFactory
    (make-reader [this _] this)
    (make-input-stream [this _] this)
    (make-writer [this _] this)
    (make-output-stream [this _] this)))


(defn ^Boolean isFd? "is File-descriptor?"
  [path]
  (= path (unsigned-bit-shift-right path 0)))

(defn filepath-dispatch [f {:keys [fd]}]
  (if (isFd? fd) :fd (type f)))


(defmulti filepath filepath-dispatch) ;should check path validity, URI too
(defmethod filepath :fd [fd ] nil)
(defmethod filepath js/String [pathstring ] pathstring)
(defmethod filepath Uri [u] (.getPath u))
(defmethod filepath :File [file] (.getPath file))
(defmethod filepath :default [x] (throw (js/Error.
                                         (str "Unrecognized path configuration passed to FileStream constructor."
                                              "\nYou passed " (pr-str x)
                                              "\nYou must pass a [pathstring], [uri], [file], or include :fd in opts .\n" )))) ;throw?

(defn valid-opts [opts]
  (clj->js (merge {:encoding "utf8"} opts)))
; kewyword for buffer instead of ""?

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn FileInputStream* [src opts]
  (let [filestreamobj (.createReadStream fs (filepath src) (valid-opts opts))
        filedesc      (atom nil)
        _             (set! (.-constructor filestreamobj) :FileInputStream)
        _             (.on filestreamobj "open" (fn [fd] (reset! filedesc fd )))]
    (specify! filestreamobj
      ;pr rep here
      ; IEquiv etc
      Object
      (getFd [_] @filedesc))
    (input-IOF! filestreamobj)))

(defn FileInputStream
  ([file] (FileInputStream file nil))
  ([file opts] (FileInputStream* file opts)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn FileOutputStream* [src {:keys [append flags] :as opts}]
  (let [flag          (or flags (if append "a" "w"))
        vopts         (valid-opts (assoc opts :flags flag))
        filestreamobj (.createWriteStream fs (filepath src) vopts)
        filedesc      (atom nil)
        _             (set! (.-constructor filestreamobj) :FileOutputStream)
        _             (.on filestreamobj "open" (fn [fd] (reset! filedesc fd )))]
    (specify! filestreamobj
      Object
      (getFd [_] @filedesc))
    (output-IOF! filestreamobj)))

(defn FileOutputStream
  ([file] (FileOutputStream file nil))
  ([file opts] (FileOutputStream* file opts)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ReadableStream
  [{:keys [read] :as opts}]
  (assert (map? opts) "you must pass a map of constructor options containing at least a :read k-v pair")
  (assert (fn? read) "you must supply an internal :read function when creating a read stream")
  (input-IOF! (new stream.Readable (clj->js opts))))

(defn WritableStream
  [{:keys [write] :as opts}]
  (assert (map? opts) "you must pass a map of constructor options containing at least a :write k-v pair")
  (assert (fn? write) "you must supply an internal :write function when creating writable streams")
  (output-IOF! (new stream.Writable (clj->js opts))))

(defn DuplexStream
  [{:keys [read write] :as opts}]
  (assert (map? opts) "you must pass a map of constructor options containing at least :read & :write fns")
  (assert (and (fn? read) (fn? write)) "you must supply :read & :write fns when creating duplex streams.")
  (duplex-IOF! (new stream.Duplex (clj->js opts))))

(defn TransformStream
  [{:keys [transform flush] :as opts}]
  (assert (map? opts) "you must pass a map of constructor options containing at least a :transform fn")
  (assert (fn? transform) "you must supply a :transform fn when creating a transform stream.")
  (assert (if flush (fn? flush) true) ":flush must be a fn")
  (duplex-IOF! (new stream.Transform (clj->js opts))))
