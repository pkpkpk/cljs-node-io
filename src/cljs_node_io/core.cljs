(ns cljs-node-io.core
  (:require-macros [cljs-node-io.macros :refer [with-open]])
  (:require [cljs.nodejs :as nodejs :refer [require]]
            [cljs.reader :refer [read-string]]
            [cljs-node-io.file :refer [File]]
            ; [cljs-node-io.reader :refer [reader]]
            [cljs-node-io.util :refer [Coercions as-url as-file
                                       IOFactory make-reader make-writer make-input-stream make-output-stream]]

            [clojure.string :as st]
            [goog.string :as gstr])
  (:import goog.Uri
           [goog.string StringBuffer] ))


(nodejs/enable-util-print!)


(def fs (require "fs"))
(def path (require "path"))
(def Buffer (.-Buffer (require "buffer")))

(extend-protocol Coercions
  nil
  (as-file [_] nil)
  (as-url [_] nil)
  string
  (as-file [s] (File. s))
  (as-url [s] (.getPath (Uri. s)))
  Uri
  (as-url [u] (.getPath u))
  (as-file [u]
    (if (= "file" (.getScheme u))
      (as-file (.getPath u)) ;goog.Uri handles decoding woohoo
      (throw (js/Error. (str "IllegalArgumentException : Not a file: " u))))))

(defn ^String as-relative-path
  "Take an as-file-able thing and return a string if it is
   a relative path, else IllegalArgumentException."
  [x]
  (let [^File f (as-file x)]
    (if (.isAbsolute f)
      (throw (js/Error. (str "IllegalArgumentException: " f " is not a relative path")))
      (.getPath f))))


(defn ^File file
  "Returns a reified file, passing each arg to as-file.  Multiple-arg
   versions treat the first argument as parent and subsequent args as
   children relative to the parent."
  ([arg]
   (as-file arg))
  ([parent child]
   (File. ^File (as-file parent) ^String (as-relative-path child)))
  ([parent child & more]
   (reduce file (file parent child) more)))

(defn ^Reader reader
  "Attempts to coerce its argument into an open java.io.Reader.
   Default implementations always return a java.io.BufferedReader.
   Default implementations are provided for Reader, BufferedReader,
   InputStream, File, URI, URL, Socket, byte arrays, character arrays,
   and String.
   If argument is a String, it tries to resolve it first as a URI, then
   as a local file name.  URIs with a 'file' protocol are converted to
   local file names.
   Should be used inside with-open to ensure the Reader is properly
   closed."
  {:added "1.2"}
  [x & opts]
  (make-reader x (when opts (apply hash-map opts))))

(defn ^Writer writer
  "Attempts to coerce its argument into an open java.io.Writer.
   Default implementations always return a java.io.BufferedWriter.
   Default implementations are provided for Writer, BufferedWriter,
   OutputStream, File, URI, URL, Socket, and String.
   If the argument is a String, it tries to resolve it first as a URI, then
   as a local file name.  URIs with a 'file' protocol are converted to
   local file names.
   Should be used inside with-open to ensure the Writer is properly
   closed."
  {:added "1.2"}
  [x & opts]
  (make-writer x (when opts (apply hash-map opts))))

(defn ^InputStream input-stream
  "Attempts to coerce its argument into an open java.io.InputStream.
   Default implementations always return a java.io.BufferedInputStream.
   Default implementations are defined for InputStream, File, URI, URL,
   Socket, byte array, and String arguments.
   If the argument is a String, it tries to resolve it first as a URI, then
   as a local file name.  URIs with a 'file' protocol are converted to
   local file names.
   Should be used inside with-open to ensure the InputStream is properly
   closed."
  {:added "1.2"}
  [x & opts]
  (make-input-stream x (when opts (apply hash-map opts))))

(defn ^OutputStream output-stream
  "Attempts to coerce its argument into an open java.io.OutputStream.
   Default implementations always return a java.io.BufferedOutputStream.
   Default implementations are defined for OutputStream, File, URI, URL,
   Socket, and String arguments.
   If the argument is a String, it tries to resolve it first as a URI, then
   as a local file name.  URIs with a 'file' protocol are converted to
   local file names.
   Should be used inside with-open to ensure the OutputStream is
   properly closed."
  {:added "1.2"}
  [x & opts]
  (make-output-stream x (when opts (apply hash-map opts))))


(defn- ^Boolean append? [opts]
  (boolean (:append opts)))

(defn- ^String encoding [opts]
  (or (:encoding opts) "UTF-8")) ;<=== utf8?

(defn- buffer-size [opts]
  (or (:buffer-size opts) 1024)) ;<==?




  ; (make-reader [x opts] (make-reader (make-input-stream x opts) opts))
  ; (make-writer [x opts] (make-writer (make-output-stream x opts) opts))
  ; (make-input-stream [x opts]
  ;                     (throw (js/Error.
  ;                             (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str x) "> as an InputStream."))))
  ; (make-output-stream [x opts]
  ;                      (throw (js/Error.
  ;                              (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str x) "> as an OutputStream."))))




(deftype char-array-type [])
(deftype CharArrayReader [x])
(deftype byte-array-type [])
(deftype ByteArrayInputStream [x])
(deftype Socket [])
(deftype FileInputStream [x])
(deftype BufferedOutputStream [x])
(deftype OutputStreamWriter [x y])
(deftype OutputStream [x])
(deftype InputStreamReader [x y])
(deftype BufferedWriter [x])
(deftype Writer [])
(deftype BufferedReader [x])
(deftype Reader [])
(deftype InputStream [])
(deftype BufferedInputStream [x])

(defn- inputstream->reader
  [^InputStream is opts]
  (make-reader (InputStreamReader. is (encoding opts)) opts))

(defn- outputstream->writer
  [^OutputStream os opts]
  (make-writer (OutputStreamWriter. os (encoding opts)) opts))






(extend-protocol IOFactory

  BufferedInputStream
  (make-reader [x opts] (inputstream->reader x opts))
  (make-writer [x opts] (make-writer (make-output-stream x opts) opts))
  (make-input-stream [x opts] x)
  (make-output-stream [x opts]
                       (throw (js/Error.
                               (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str x) "> as an OutputStream."))))

  InputStream
  (make-reader [x opts] (inputstream->reader x opts))
  (make-writer [x opts] (make-writer (make-output-stream x opts) opts))
  (make-input-stream [x opts] (BufferedInputStream. x))
  (make-output-stream [x opts]
                       (throw (js/Error.
                               (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str x) "> as an OutputStream."))))

  Reader
  (make-reader [x opts] (BufferedReader. x))
  (make-writer [x opts] (make-writer (make-output-stream x opts) opts))
  (make-input-stream [x opts]
                      (throw (js/Error.
                              (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str x) "> as an InputStream."))))
  (make-output-stream [x opts]
                       (throw (js/Error.
                               (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str x) "> as an OutputStream."))))

  BufferedReader
  (make-reader [x opts] x)
  (make-writer [x opts] (make-writer (make-output-stream x opts) opts))
  (make-input-stream [x opts]
                      (throw (js/Error.
                              (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str x) "> as an InputStream."))))
  (make-output-stream [x opts]
                       (throw (js/Error.
                               (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str x) "> as an OutputStream."))))

  Writer
  (make-reader [x opts] (make-reader (make-input-stream x opts) opts))
  (make-writer [x opts] (BufferedWriter. x))
  (make-input-stream [x opts]
                      (throw (js/Error.
                              (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str x) "> as an InputStream."))))
  (make-output-stream [x opts]
                       (throw (js/Error.
                               (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str x) "> as an OutputStream."))))

  BufferedWriter
  (make-reader [x opts] (make-reader (make-input-stream x opts) opts))
  (make-writer [x opts] x)
  (make-input-stream [x opts]
                      (throw (js/Error.
                              (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str x) "> as an InputStream."))))
  (make-output-stream [x opts]
                      (throw (js/Error.
                               (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str x) "> as an OutputStream."))))

  OutputStream
  (make-reader [x opts] (make-reader (make-input-stream x opts) opts))
  (make-writer [x opts] (outputstream->writer x opts))
  (make-input-stream [x opts]
                        (throw (js/Error.
                                (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str x) "> as an InputStream."))))
  (make-output-stream [x opts] (BufferedOutputStream. x))

  BufferedOutputStream
  (make-reader [x opts] (make-reader (make-input-stream x opts) opts))
  (make-writer [x opts] (outputstream->writer x opts))
  (make-input-stream [x opts]
                      (throw (js/Error.
                              (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str x) "> as an InputStream."))))
  (make-output-stream [x opts] x)

  Uri
  (make-reader [x opts] (make-reader (make-input-stream x opts) opts))
  (make-writer [x opts] (make-writer (make-output-stream x opts) opts))
  (make-input-stream [x opts] (make-input-stream
                                (if (= "file" (.getScheme x))
                                  (FileInputStream. (as-file x))
                                  (.openStream x)) opts))
  (make-output-stream [x opts] (if (= "file" (.getScheme x))
                                 (make-output-stream (as-file x) opts)
                                 (throw (js/Error. (str "IllegalArgumentException: Can not write to non-file URL <" x ">")))))

  string
  (make-reader [x opts] (make-reader (make-input-stream x opts) opts))
  (make-writer [x opts] (make-writer (make-output-stream x opts) opts))
  (make-input-stream [^String x opts](try
                                        (make-input-stream (Uri. x) opts)
                                        (catch js/Error e ;MalformedURLException
                                          (make-input-stream (File. x) opts))))
  (make-output-stream [^String x opts] (try
                                        (make-output-stream (Uri. x) opts)
                                          (catch js/Error err ;MalformedURLException
                                              (make-output-stream (File. x) opts))))

  Socket
  (make-reader [x opts] (make-reader (make-input-stream x opts) opts))
  (make-writer [x opts] (make-writer (make-output-stream x opts) opts))
  (make-input-stream [^Socket x opts] (make-input-stream (.getInputStream x) opts))
  (make-output-stream [^Socket x opts] (make-output-stream (.getOutputStream x) opts))

  byte-array-type
  (make-reader [x opts] (make-reader (make-input-stream x opts) opts))
  (make-writer [x opts] (make-writer (make-output-stream x opts) opts))
  (make-input-stream [x opts] (make-input-stream (ByteArrayInputStream. x) opts))
  (make-output-stream [x opts]
                       (throw (js/Error.
                               (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str x) "> as an OutputStream."))))

  char-array-type
  (make-reader [x opts] (make-reader (CharArrayReader. x) opts))
  (make-writer [x opts] (make-writer (make-output-stream x opts) opts))
  (make-input-stream [x opts]
                        (throw (js/Error.
                                (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str x) "> as an InputStream."))))
  (make-output-stream [x opts]
                         (throw (js/Error.
                                 (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str x) "> as an OutputStream."))))
  object
  (make-reader [x opts] (make-reader (make-input-stream x opts) opts))
  (make-writer [x opts] (make-writer (make-output-stream x opts) opts))
  (make-input-stream [x opts]
                      (throw (js/Error.
                              (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str x) "> as an InputStream."))))
  (make-output-stream [x opts]
                       (throw (js/Error.
                               (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str x) "> as an OutputStream.")))))



(defn- normalize-slurp-opts
  [opts]
  (if (string? (first opts))
    (do
      (println "WARNING: (slurp f enc) is deprecated, use (slurp f :encoding enc).")
      [:encoding (first opts)])
    opts))

(defn slurp
  "Opens a reader on f and reads all its contents, returning a string.
  See clojure.java.io/reader for a complete list of supported arguments."
  {:added "1.0"}
  ([f & opts]
     (let [opts (normalize-slurp-opts opts)
           sb (StringBuffer.)]
       (with-open [r (apply reader f opts)]
         (loop [c (.read r)]
           (if (neg? c);;;????? should be null for nodejs?
             (.toString sb)
             (do
               (.append sb (char c))
               (recur (.read r)))))))))

(defn spit
  "Opposite of slurp.  Opens f with writer, writes content, then
  closes f. Options passed to clojure.java.io/writer."
  [f content & options]
  (with-open [w (apply writer f options)]
    (.write w (str content))))

(defn -main [& args] nil)
(set! *main-cli-fn* -main)
