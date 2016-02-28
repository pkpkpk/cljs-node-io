(ns cljs-node-io.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [cljs.nodejs :as nodejs :refer [require]]
            [cljs.core.async :as async :refer [put! take! chan <! pipe  alts!]]
            [cljs-node-io.file :refer [File]]
            ; [cljs-node-io.reader :refer [reader]]
            [cljs.reader :refer [read-string]]
            [cljs-node-io.streams :refer [FileInputStream]]
            [cljs-node-io.protocols
              :refer [Coercions as-url as-file
                      IOFactory make-reader make-writer make-input-stream make-output-stream]]
            [clojure.string :as st]
            [goog.string :as gstr])
  (:import goog.Uri
           [goog.string StringBuffer]))

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

(defn delete-file
  "Delete file f. Raise an exception if it fails unless silently is true."
  [f & [silently]]
  (or (.delete (file f))
      silently
      (throw (js/Error. (str "Couldn't delete " f)))))

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
  (or (:encoding opts) "utf8"))

(defn- buffer-size [opts]
  (or (:buffer-size opts) 1024)) ;<==?



(extend-protocol IOFactory
  Uri
  (make-reader [x opts] (make-reader (make-input-stream x opts) opts))
  (make-writer [x opts] (make-writer (make-output-stream x opts) opts))
  (make-input-stream [x opts] (make-input-stream
                                (if (= "file" (.getScheme x)) ;move this to make-reader?
                                  (FileInputStream. (as-file x))
                                  (.openStream x)) opts))
  (make-output-stream [x opts] (if (= "file" (.getScheme x))
                                 (make-output-stream (as-file x) opts)
                                 (throw (js/Error. (str "IllegalArgumentException: Can not write to non-file URL <" x ">")))))

  string
  (make-reader [x opts] (make-reader (as-file x) opts)); choice to make stream is handled by opts passed to reader
  (make-writer [x opts] (make-writer (as-file x) opts))
  (make-input-stream [^String x opts](try
                                        (make-input-stream (Uri. x) opts)
                                        (catch js/Error e ;MalformedURLException
                                          (make-input-stream (File. x) opts))))
  (make-output-stream [^String x opts] (try
                                        (make-output-stream (Uri. x) opts)
                                          (catch js/Error err ;MalformedURLException
                                              (make-output-stream (File. x) opts)))))

(defn error? [e] (instance? js/Error e))


(defn slurp
  "Returns String synchronously by default
   If :stream? true, punts to file-stream-reader, havent figured out yet
   If :async? true, returns channel which will receive err|data specified by encoding via put! cb
   If :reader true,  attempts to convert the file content to clj data structures
   If :encoding \"\" (an explicit empty string), returns raw buffer instead of string.
   @returns {String|Buffer|Channel}"
  ([f & opts]
   (let [r (apply reader f opts)]
     (.read r) )))

(defn aslurp
  "sugar for (slurp f :async? true ...)
   Returns a channel which will receive err|data specified by encoding via put! cb
   @returns {Channel}"
  [f & opts]
  (let [r (apply reader f (concat opts '(:async? true)))]
    (.read r)))

(defn reader-method
  "Finds an appropriate reader based on the file's extension. ie clojure.reader/read-string
   for edn files.
   Should be user extensible?"
  [filepath]
  (condp = (.extname path filepath)
    ".edn"  (fn [contents] (read-string contents))
    ".json" (fn [contents] (js->clj (js/JSON.parse contents) :keywordize-keys true))
    ;xml, csv, transit?
    ;; does it make sense to throw here?
    (throw (js/Error. "sslurp was given an unrecognized file format.
                       The file's extension must be json or edn"))))


(defn sslurp
  "augmented 'super' slurp for convenience. edn|json => clj data-structures"
  [f & opts]
  (let [ff    (apply reader f opts)
        rdr  (reader-method (.getPath ff))]
    (rdr (.read ff))))

(defn saslurp
  "augmented 'super' aslurp for convenience. edn|json => clj data-structures put into a ch
    TODO: allow passing custom reader fn
   @returns {Channel} which receives edn data or error "
  [f & opts]
  (let [file  (apply reader f (concat opts '(:async? true :reader true)))
        rdr   (reader-method (.getPath file))
        from  (.read file)
        to    (chan 1 (map #(if (error? %) % (rdr %))) )
        _     (pipe from to)]
    to))

(defn spit
  "Opposite of slurp.  Opens f with writer, writes content.
   Options passed to a file/file-writer.
   :encoding
   :append
   :async?
   :stream?
  "
  [f content & options]
  (let [w (apply writer f options)]
    (.write w (str content))))

(defn aspit
  "Async spit. returned chan recieves error or true on write success.
   Wait for result before writing again.
   @returns {Channel}"
  [f content & options]
  (let [w (apply writer f (concat options '(:async? true)))]
    (.write w (str content))))

; (defn line-seq
;   "Returns the lines of text from rdr as a lazy sequence of strings.
;   rdr must implement java.io.BufferedReader."
;   {:static true}
;   [^java.io.BufferedReader rdr]
;   (when-let [line (.readLine rdr)]
;     (cons line (lazy-seq (line-seq rdr)))))


(defn file-seq
  "taken from clojurescript/examples/nodels.cljs"
  [dir]
  (tree-seq
    (fn [f] (.isDirectory (.statSync fs f) ))
    (fn [d] (map #(.join path d %) (.readdirSync fs d)))
    dir))

(defn xml-seq
  "A tree seq on the xml elements as per xml/parse"
  [root]
    (tree-seq
     (complement string?)
     (comp seq :content)
     root))

(defn make-parents
  "Given the same arg(s) as for file, creates all parent directories of
   the file they represent."
  [f & more]
  (when-let [parent (.getParentFile ^File (apply file f more))]
    (.mkdirs parent)))



(defn do-copy [input output opts]
  (let [in  (make-input-stream input opts)
        out (make-output-stream output opts)]
    (try
      (do (.pipe in out) nil)
      (catch js/Error e (throw e)))))

(defn copy
  "Copies input to output.  Returns nil or throws IOException.
  Input may be an InputStream, Reader, File, byte[], or String.
  Output may be an OutputStream, Writer, or File.
  Options are key/value pairs and may be one of
    :buffer-size  buffer size to use, default is 1024.
    :encoding     encoding to use if converting between
                  byte and char streams.
  Does not close any streams except those it opens itself
  (on a File)."
  ; :stream? option to use async stream readers vs sync
  [input output & opts]
  (do-copy input output (when opts (apply hash-map opts))))

(defn -main [& args] nil)
(set! *main-cli-fn* -main)
