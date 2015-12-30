(ns cljs-node-io.core
  ; (:require-macros [cljs-node-io.macros :refer []])
  (:require [cljs.nodejs :as nodejs :refer [require]]
            [cljs.reader :refer [read-string]]
            ; [goog.uri.utils :as uri-utils]

            [cljs-node-io.file :refer [File]]
            [cljs-node-io.util :refer [Coercions as-url as-file]]

            [clojure.string :as st]
            [goog.string :as gstr])
  (:import goog.Uri))

#_(comment
    1. convert tests
    2. cookbook io examples)
; error handling can be better, move away from generic js/Error.
; consolidated URL & URI
; java.io.File's  constructor is problematic
; gcl node-object-stream support

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



(defprotocol ^{:added "1.2"} IOFactory
  "Factory functions that create ready-to-use, buffered versions of
   the various Java I/O stream types, on top of anything that can
   be unequivocally converted to the requested kind of stream.
   Common options include

     :append    true to open stream in append mode
     :encoding  string name of encoding to use, e.g. \"UTF-8\".
   Callers should generally prefer the higher level API provided by
   reader, writer, input-stream, and output-stream."
  (make-reader [x opts] "Creates a BufferedReader. See also IOFactory docs.")
  (make-writer [x opts] "Creates a BufferedWriter. See also IOFactory docs.")
  (make-input-stream [x opts] "Creates a BufferedInputStream. See also IOFactory docs.")
  (make-output-stream [x opts] "Creates a BufferedOutputStream. See also IOFactory docs."))



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


(def default-streams-impl
  {:make-reader (fn [x opts] (make-reader (make-input-stream x opts) opts))
   :make-writer (fn [x opts] (make-writer (make-output-stream x opts) opts))
   :make-input-stream (fn [x opts]
                        (throw (js/Error.
                                (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str x) "> as an InputStream."))))
   :make-output-stream (fn [x opts]
                         (throw (js/Error.
                                 (str "ILLEGAL ARGUMENT: Cannot open <" (pr-str x) "> as an OutputStream."))))})














(def foo (reify Object (close [_] (println "closing"))))


(defn sslurp
  "augmented slurp for convenience. edn|json => clj data-structures"
  [filename]
  (let [contents (.readFileSync fs filename "utf8")]
    (condp = (.extname path filename)
      ".edn"  (read-string contents)
      ".json" (js->clj (js/JSON.parse contents) :keywordize-keys true)
      ;xml, csv, disable-auto-reading?
      (throw (js/Error. "sslurp was given an unrecognized file format.
                         The file's extension must be json or edn")))))



; clj uses protocols for all kinds of types, see clojure.java.io/writer
; https://github.com/clojure/clojure/blob/clojure-1.7.0/src/clj/clojure/java/io.clj
; writer, bufferedwriter, outstream, bufferedoutstream, url, uri, file, byte-array, socket,
; other opts: encoding, flags, mode, buffer-size, sync vs async, others?
; flags: w=overwrite, a=append. FS.appendFileSync is just wrapper over
(defn spit [filename content & opts]
  (let [opts (apply hash-map opts)]
    (if (:append? opts) ;should check encoding, mode, sync too?
      (.writeFileSync fs filename content  #js{"flag" "a"}) ;{ encoding: 'utf8', mode: 0o666, flag: 'a' }
      (.writeFileSync fs filename content  #js{"flag" "w"}))))










; takes path returns file descriptor?
;null checks  on path

(defn open-file [path flag mode]
  (.openSync fs path flag mode))




;node is polymorphic on path, can be string or file-descriptor,
;if path => call fs.open on it
;if fd => leave as is

(defn read-file ;sync
  [^String path & {:keys [encoding flag]  :or {encoding nil flag "r"}  :as opts}]
  (let [mode  (js/parseInt "0o666" 8)


        fd    (open-file path flag nil)
        st    (.fstatSync fs fd)
        size  (if (.isFile st) (.size st) 0)

        ;; setup buffers
        pos   0
        buffers (if (zero? size) #js[] )
        buffer  (if-not (zero? size) (Buffer. size))

        ;; read bytes into buffer

      ]
    nil))




































(defn -main [& args] nil)
(set! *main-cli-fn* -main)
