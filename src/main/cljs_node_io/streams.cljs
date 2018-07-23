(ns cljs-node-io.streams
  (:import goog.Uri)
  (:require [cljs.core.async :as async :refer [put! take! chan <! pipe  alts!]]
            [cljs-node-io.protocols
              :refer [Coercions as-url as-file
                      IInputStream IOutputStream IFile
                      IOFactory make-reader make-writer make-input-stream make-output-stream]]))

(def fs (js/require "fs"))
(def stream (js/require "stream"))

(defn input-IOF!
  "adds IOFactory input impls that just defer back to the stream or throw as appropriate
   @param {!stream.Readable} streamobj
   @return {!stream.Readable}"
  [streamobj]
  (specify! streamobj
    IInputStream
    IOFactory
    (make-reader [this opts] this)
    (make-input-stream [this _] this)
    (make-writer [this _] (throw (js/Error. (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str this) "> as an OutputStream."))))
    (make-output-stream [this _] (throw (js/Error. (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str this) "> as an OutputStream."))))))

(defn output-IOF!
  "adds IOFactory output impls that just defer back to the stream or throw as appropriate
   @param {!stream.Writable} streamobj
   @return {!stream.Writable}"
  [streamobj]
  (specify! streamobj
    IOutputStream
    IOFactory
    (make-reader [this _] (throw (js/Error. (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str this) "> as an InputStream."))))
    (make-input-stream [this _] (throw (js/Error. (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str this) "> as an InputStream."))))
    (make-writer [this _] this)
    (make-output-stream [this _] this)))

(defn duplex-IOF!
  "defer back to the stream in all cases
   @param {!stream.Duplex} streamobj
   @return {!stream.Duplex}"
  [streamobj]
  (specify! streamobj
    IInputStream
    IOutputStream
    IOFactory
    (make-reader [this _] this)
    (make-input-stream [this _] this)
    (make-writer [this _] this)
    (make-output-stream [this _] this)))


(defn ReadableStream
  "@param {!IMap} opts
   @return {!stream.Readable}"
  [opts]
  (assert (map? opts) "you must pass a map of constructor options containing at least a :read k-v pair")
  (assert (fn? (get opts :read)) "you must supply an internal :read function when creating a read stream")
  (input-IOF! (new stream.Readable (clj->js opts))))

(defn WritableStream
  "@param {!IMap} opts
   @return {!stream.Writable}"
  [opts]
  (assert (map? opts) "you must pass a map of constructor options containing at least a :write k-v pair")
  (assert (fn? (get opts :write)) "you must supply an internal :write function when creating writable streams")
  (output-IOF! (new stream.Writable (clj->js opts))))

(defn DuplexStream
  "@param {!IMap} opts
   @return {!stream.Duplex}"
  [opts]
  (assert (map? opts) "you must pass a map of constructor options containing at least :read & :write fns")
  (assert (and (fn? (get opts :read)) (fn? (get opts :write))) "you must supply :read & :write fns when creating duplex streams.")
  (duplex-IOF! (new stream.Duplex (clj->js opts))))

(defn TransformStream
  "@param {!IMap} opts
   @return {!stream.Duplex}"
  [opts]
  (assert (map? opts) "you must pass a map of constructor options containing at least a :transform fn")
  (assert (fn? (get opts :transform)) "you must supply a :transform fn when creating a transform stream.")
  (assert (if-let [flush (get opts :flush)] (fn? flush) true) ":flush must be a fn")
  (duplex-IOF! (new stream.Transform (clj->js opts))))

(defn BufferReadStream
  "Creates a ReadableStream from a Buffer. Opts are same as ReadableStream except
   the :read fn is provided. If you provide :read, it is ignored
   @return {!stream.Readable}"
  ([source](BufferReadStream source nil))
  ([source opts]
   (assert (js/Buffer.isBuffer source) "source must be a Buffer instance")
   (let [offset (atom 0)
         length (.-length source)
         read   (fn [size]
                  (this-as this
                   (if (< @offset length)
                     ; still buffer to consume
                     (let [chunk (.slice source @offset (+ @offset size))]
                       (.push this chunk)
                       (swap! offset + size))
                     ; offset>=buffer length...totally consumed
                     (.push this nil))))
         strm (ReadableStream (merge opts {:read read}))]
     (input-IOF! strm))))

(defn BufferWriteStream
  "Creates WritableStream to a buffer. The buffer is formed from concatenated
   chunks passed to write method. cb is called with the buffer on the 'finish' event.
   'finish' must be triggered to recieve buffer
   @return {!stream.Writable}"
  ([cb] (BufferWriteStream cb nil))
  ([cb opts]
   (let [data  #js[]
         buf   (atom nil)
         write (fn [chunk _ callback]
                 (.push data chunk)
                 (callback))
         strm  (WritableStream (merge opts {:write write}))
         _     (set! (.-buf strm) data)
         _     (.on strm "finish"
                (fn []
                  (let [b (js/Buffer.concat data)]
                    (reset! buf b)
                    (cb b))))]
     (specify! (output-IOF! strm)
      Object
      ; (destroy [this] )
      (toString [_] (if @buf (.toString @buf)))
      (toBuffer [_] @buf)))))

(defn- ^boolean fd?
  "@param {!Number} fd
   @return {!boolean} is File-descriptor?"
   [fd]
   (= fd (unsigned-bit-shift-right fd 0)))

(defn- filepath
  "@param {(string|Uri|IFile)} f :: path to filestream
   @param {?IMap} opts :: map of options
   @param {!string} k :: string provided by caller for more detailed error in else case
   @return {?string} or throws. returns nil if fd is present in opts, otherwise returns pathstring"
  [f opts k]
  (let [fd (get opts :fd)]
    (cond
      (fd? fd) nil
      (string? f) f
      (or (implements? IFile f) (= Uri (type f))) (.getPath f)
      :else
      (throw (js/TypeError.
              (str "Unrecognized path configuration passed to File" k "Stream constructor."
                   "\n    You passed " (pr-str f) " and " (pr-str opts)
                   "\n    You must pass a [pathstring opts], [uri opts], [file opts], or include :fd in opts ." ))))))

(defn- FileInputStream*
  "@param {!string} src :: filepath to read from
   @param {!IMap} opts :: map of options
   @return {!stream.Readable}"
  [src opts]
  (let [{:keys [flags encoding fd mode autoClose?]} opts
        options #js {"encoding" (or encoding nil)
                     "flags" (or flags "r")
                     "fd" (or fd nil)
                     "mode" (or mode 438)
                     "autoClose" (or autoClose? true)}
        filestreamobj (.createReadStream fs src options)
        filedesc      (atom nil)
        _             (.on filestreamobj "open" (fn [fd] (reset! filedesc fd )))]
    (specify! filestreamobj
      IInputStream
      IEquiv
      (-equiv [this that] (and (= (type this) (type that)) (= (.-path this) (.-path that))))
      IPrintWithWriter
      (-pr-writer [this writer opts]
        (-write writer "#object [FileInputStream")
        (-write writer (str "  "  (.-path this)  "]")))
      Object
      (getFd [_] @filedesc))
    (input-IOF! filestreamobj)))

(defn FileInputStream
  "@return {!stream.Readable}"
  ([src] (FileInputStream src nil))
  ([src opts] (FileInputStream* (filepath src opts "Input") opts)))


(defn- FileOutputStream*
  "@param {!string} target :: filepath to write to
   @param {!IMap} opts :: map of options
   @return {!stream.Writable}"
  [target opts]
  (let [{:keys [append flags encoding mode fd]} opts
        options  #js {"defaultEncoding" (or encoding "utf8")
                      "flags" (or flags (if append "a" "w"))
                      "fd" (or fd nil)
                      "mode" (or mode 438)}
        filestreamobj (.createWriteStream fs target options)
        filedesc      (atom nil)
        _             (.on filestreamobj "open" (fn [fd] (reset! filedesc fd )))]
    (specify! filestreamobj
      IOutputStream
      IEquiv
      (-equiv [this that] (and (= (type this) (type that)) (= (.-path this) (.-path that))))
      IPrintWithWriter
      (-pr-writer [this writer opts]
        (-write writer "#object [FileOutputStream")
        (-write writer (str "  "  (.-path this)  "]")))
      Object
      (getFd [_] @filedesc))
    (output-IOF! filestreamobj)))

(defn FileOutputStream
  "@return {!stream.Writable}"
  ([target] (FileOutputStream target nil))
  ([target opts](FileOutputStream* (filepath target opts "Output") opts)))
