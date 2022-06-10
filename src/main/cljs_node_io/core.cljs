(ns cljs-node-io.core
  (:require [cljs.core.async :as async :refer [put! take! promise-chan close!]]
            [cljs.core.async.impl.protocols :refer [Channel]]
            [cljs-node-io.file :refer [File]]
            [cljs-node-io.fs :as iofs])
  (:import goog.Uri))

(def fs (js/require "fs"))
(def path (js/require "path"))
(def stream (js/require "stream"))

(defn ^boolean Buffer?
  "sugar over Buffer.isBuffer
   @param {*} b
   @return {!boolean}"
  [b]
  (js/Buffer.isBuffer b))

(def buffer? Buffer?)

(extend-type js/Buffer
  IEquiv
  (-equiv [this o]
    (if (identical? this o)
      true
      (try
        (.equals this o)
        (catch js/Error _
          false)))))

(defn filepath
  "This is needed to mock the java.io.File constructor.
   The java File constructor is polymorphic and accepts one or two args:
   (Uri), (pathstring), (parentstring, childstring), (File, childstring)
   @return {!string}"
  ([a] (filepath a nil))
  ([a b]
   (condp = [(type a) (type b)]
     [Uri nil] (.getPath a)
     [js/String nil] a
     [js/String js/String] (path.join a b)
     [File js/String] (path.join (.getPath a) b)
     :else
     (throw
       (js/TypeError.
        (str "Unrecognized path configuration passed to File constructor."
             "\nYou passed " (pr-str a) " and " (pr-str b)
             "\nYou must pass a [string], [uri], [string string], or [file string]."))))))

;;==============================================================================

(defprotocol Coercions
  (as-file [x] "Coerce argument to a file.")
  (as-url [x] "Coerce argument to a URL."))

(extend-protocol Coercions
  nil
  (as-file [_] nil)
  (as-url [_] nil)
  string
  (as-file [s] (File. (filepath s)))
  (as-url [s] (.getPath (Uri. s)))
  File
  (as-file [f] f)
  (as-url [f] (.to-url f))
  Uri
  (as-url [u] (.getPath u))
  (as-file [u]
    (if (= "file" (.getScheme u))
      (as-file (.getPath u))
      (throw (js/Error. (str "Uri's must have file protocol: " u))))))

;;==============================================================================

(defprotocol IOFactory
  "Attempts to create a native node stream."
  (make-reader [x opts] "create stream.Readable")
  (make-writer [x opts] "create stream.Writable")
  (make-input-stream [x opts] "create stream.Readable")
  (make-output-stream [x opts] "create stream.Writable"))

(defn- default-make-input-stream
  [x opts]
  (throw (js/Error. (str "Cannot open <" (pr-str x) "> as stream.Readable"))))

(defn- default-make-output-stream
  [x opts]
  (throw (js/Error. (str "Cannot open <" (pr-str x) "> as stream.Writable"))))

(extend-protocol IOFactory
  nil
  (make-reader [x opts] (default-make-input-stream x opts))
  (make-writer [x opts] (default-make-output-stream x opts))
  (make-input-stream [x opts] (default-make-input-stream x opts))
  (make-output-stream [x opts] (default-make-output-stream x opts))
  stream.Readable
  (make-reader [x opts] x)
  (make-writer [x opts] (make-output-stream x opts))
  (make-input-stream [x opts] x)
  (make-output-stream [x opts] (default-make-output-stream x opts))
  stream.Writable
  (make-reader [x opts] (make-input-stream x opts))
  (make-writer [x opts] x)
  (make-input-stream [x opts] (default-make-input-stream x opts))
  (make-output-stream [x opts] x)
  stream.Duplex
  (make-reader [x opts] x)
  (make-writer [x opts] x)
  (make-input-stream [x opts] x)
  (make-output-stream [x opts] x)
  string
  (make-reader [x opts] (make-input-stream x opts))
  (make-writer [x opts] (make-output-stream x opts))
  (make-input-stream [x opts] (fs.createReadStream x (clj->js opts)))
  (make-output-stream [x opts] (fs.createWriteStream x (clj->js opts)))
  File
  (make-reader [x opts] (make-input-stream x opts))
  (make-writer [x opts] (make-output-stream x opts))
  (make-input-stream [x opts] (fs.createReadStream (.getPath x) (clj->js opts)))
  (make-output-stream [x opts] (fs.createWriteStream (.getPath x) (clj->js opts)))
  js/Buffer
  (make-reader [x opts] (make-input-stream x opts))
  (make-writer [x opts] (make-output-stream x opts))
  (make-input-stream [x opts]
    (let [rs (stream.Readable. (clj->js opts))]
      (set! (.-_read rs) (fn []))
      (.push rs x)
      (.push rs nil)
      rs))
  (make-output-stream [x opts] (default-make-output-stream x opts))
  Uri ; only file at this time
  (make-reader [x opts] (make-reader (make-input-stream x opts) opts))
  (make-writer [x opts] (make-writer (make-output-stream x opts) opts))
  (make-input-stream [x opts]
   (if (= "file" (.getScheme x))
     (make-input-stream (.getPath x) x)
     (default-make-input-stream x opts)))
  (make-output-stream [x opts]
    (if (= "file" (.getScheme x))
      (make-output-stream (.getPath x) x)
      (default-make-output-stream x opts))))

(defn reader
  "Attempt to create a native stream.Readable.
   {@link https://nodejs.org/api/stream.html#class-streamreadable}
   Note that node streams are event driven.
     + stream.Readable => itself
     + buffers => stream.Readable
     + files + strings + goog.Uri => fs.ReadStream
   @return {!stream.Readable}"
  [x & opts]
  (make-reader x (when opts (apply hash-map opts))))

(defn writer
  "Attempt to create a native stream.Writable.
   {@link https://nodejs.org/api/stream.html#class-streamwritable}
   Note that node streams are event driven.
     + stream.Writable => itself
     + buffers => stream.Writable
     + files + strings + goog.Uri => fs.WriteStream
   @return {!stream.Writable}"
  [x & opts]
  (make-writer x (when opts (apply hash-map opts))))

(defn input-stream
  "Attempt to create a native stream.Readable.
   {@link https://nodejs.org/api/stream.html#class-streamreadable}
   Note that node streams are event driven.
     + stream.Readable => itself
     + buffers => stream.Readable
     + files + strings + goog.Uri => fs.ReadStream
   @return {!stream.Readable}"
  [x & opts]
  (make-input-stream x (when opts (apply hash-map opts))))

(defn output-stream
  "Attempt to create a native stream.Writable.
   {@link https://nodejs.org/api/stream.html#class-streamwritable}
   Note that node streams are event driven.
     + stream.Writable => itself
     + buffers => stream.Writable
     + files + strings + goog.Uri => fs.WriteStream
   @return {!stream.Writable}"
  [x & opts]
  (make-output-stream x (when opts (apply hash-map opts))))

(defn readable
  "Tries to create a stream.Readable from obj. Supports iterables & array-likes
   in addition to those types supported by IOFactory
   {@link https://nodejs.org/api/stream.html#class-streamreadable}
   @param {*} obj
   @return {!stream.Readable}"
  ([obj] (readable obj {}))
  ([obj opts]
   (cond
     (goog.isArrayLike obj)
     (make-input-stream ^js/Buffer (js/Buffer.from obj) opts)

     (js-iterable? obj)
     (stream.Readable.from obj (clj->js opts))

     true
     (make-input-stream obj opts))))

(defn writable
  "Tries to create a stream.Writable from obj
   @param {*} obj
   @return {!stream.Writable}"
  ([obj] (writable obj {}))
  ([obj opts]
   (make-output-stream obj opts)))

;;==============================================================================

(defn as-relative-path
  "a relative path, else IllegalArgumentException.
   @param {(string|IFile|Uri)} x
   @return {!string}"
  [x]
  (let [f (as-file x)]
    (if (.isAbsolute f)
      (throw (js/Error. (str "IllegalArgumentException: " f " is not a relative path")))
      (.getPath f))))

(defn file
  "Returns a reified file, passing each arg to as-file.  Multiple-arg
   versions treat the first argument as parent and subsequent args as
   children relative to the parent. Use in place of File constructor
   @return {!IFile}"
  ([arg]
   (as-file arg))
  ([parent child]
   (File. (filepath (as-file parent) (as-relative-path child))))
  ([parent child & more]
   (reduce file (file parent child) more)))

(defn delete-file
  "Delete file f. Raise an exception if it fails unless silently is true.
   @return {!boolean}"
  [f & [silently]]
  (or (.delete (file f))
      silently
      (throw (js/Error. (str "Couldn't delete " f)))))

(defn slurp
  "Returns a string synchronously. Unlike JVM, does not use FileInputStream.
   Only option at this time is :encoding
   If :encoding \"\" (an explicit empty string), returns raw buffer instead of string.
   @return {(string|buffer.Buffer)}"
  [p & opts]
  (let [opts (apply hash-map opts)
        f    (as-file p)]
    (.read f (or (:encoding opts) "utf8"))))

(defn aslurp
  "@return {!Channel} a which will receive [?err ?data]"
  [p & opts]
  (let [opts (apply hash-map opts)
        f (as-file p)]
    (.aread f (or (:encoding opts) "utf8"))))

(defn spit
  "Writes content synchronously to file f.
   :encoding {string} encoding to write the string. Ignored when content is a buffer
   :append - {boolean} - if true add content to end of file
   @return {nil} or throws"
  [p content & options]
  (let [opts (apply hash-map options)
        f    (as-file p)]
    (.write f (str content) opts)))

(defn aspit
  "Async spit. Wait for result before writing again!
   @return {!Channel} recieves [?err]"
  [p content & options]
  (let [opts (apply hash-map options)
        f    (as-file p)]
    (.awrite f (str content) opts)))

(defn file-seq
  "taken from clojurescript/examples/nodels.cljs"
  [dir]
  (tree-seq
    (fn [f] (.isDirectory (file f) ))
    (fn [d] (map #(.join path d %) (.list (file d))))
    dir))

(defn make-parents
  "Given the same arg(s) as for file, creates all parent directories of
   the file they represent.
   @return {!boolean}"
  [f & more]
  (when-let [parent (.getParentFile (apply file f more))]
    (.mkdirs parent)))

(defn copy
  "Synchronously copies input to output..
   Only supports files and strings/buffers as filepaths
   @param {(string, File, Buffer)} input
   @param {(string, File, Buffer)} output
   @return {nil} throws on error"
  [input output & opts]
  (assert (or (instance? File input) (string? input) (buffer? input)))
  (assert (or (instance? File output) (string? output) (buffer? output)))
  (iofs/copy-file input output))

(defn acopy
  "Copies input to output via streams asynchronously.
   + Unlike JVM, strings interpreted as filespaths
   + Options are passed to the output stream.
   @param {(string, File, Buffer, Readable)} input
   @param {(string, File, Buffer, Readable)} output
   @return {!Channel} yielding [?err]"
  [input output & opts]
  (let [opts (when opts (apply hash-map opts))
        out (promise-chan)
        input (make-input-stream input nil)
        output (make-output-stream output opts)]
    (stream.pipeline input output #(put! out [%]))
    out))
