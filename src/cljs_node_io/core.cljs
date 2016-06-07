(ns cljs-node-io.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [cljs.nodejs :as nodejs :refer [require]]
            [cljs.core.async :as async :refer [put! take! chan <! pipe  alts!]]
            [cljs.core.async.impl.protocols :refer [Channel]]
            [cljs-node-io.file :refer [File]]
            [cljs.reader :refer [read-string]]
            [cljs-node-io.streams :refer [FileInputStream BufferReadStream] :as streams]
            [cljs-node-io.protocols
              :refer [Coercions as-url as-file IInputStream IOutputStream IFile
                      IOFactory make-reader make-writer make-input-stream make-output-stream]]
            [clojure.string :as st]
            [goog.string :as gstr])
  (:import goog.Uri
           [goog.string StringBuffer]))

(nodejs/enable-util-print!)

(def path (require "path"))

(extend-protocol IEquiv
  js/Buffer
  (-equiv [this that] (.equals this that)))

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
    (if (= "file" (.getScheme u)) ;"file://home/.../cljs-node-io/foo.edn"
      (as-file (.getPath u))
      (throw (js/Error. (str "IllegalArgumentException : Not a file: " u))))))

(extend-protocol IOFactory
  Uri
  (make-reader [x opts] (make-reader (make-input-stream x opts) opts))
  (make-writer [x opts] (make-writer (make-output-stream x opts) opts))
  (make-input-stream [x opts] (make-input-stream
                                (if (= "file" (.getScheme x))
                                  (FileInputStream. (as-file x))
                                  (.openStream x ))
                                opts)) ;<---not implemented, setup for other protocols ie HTTP
  (make-output-stream [x opts] (if (= "file" (.getScheme x))
                                 (make-output-stream (as-file x) opts)
                                 (throw (js/Error. (str "IllegalArgumentException: Can not write to non-file URL <" x ">")))))

  string
  (make-reader [x opts] (make-reader (as-file x) opts))
  (make-writer [x opts] (make-writer (as-file x) opts))
  (make-input-stream [^String x opts](try
                                        (make-input-stream (Uri. x) opts)
                                        (catch js/Error e
                                          (make-input-stream (File. x) opts))))
  (make-output-stream [^String x opts] (try
                                        (make-output-stream (Uri. x) opts)
                                          (catch js/Error err
                                              (make-output-stream (File. x) opts))))
  js/Buffer
  (make-reader [b opts] (make-reader (make-input-stream b opts) opts))
  (make-input-stream [b opts] (BufferReadStream. b opts))
  (make-writer [x opts] (make-writer (make-output-stream x opts) opts))
  (make-output-stream [x opts](throw (js/Error.  ;use Buffer.concat if you want to do this
                                 (str "IllegalArgumentException : Cannot open <" (pr-str x) "> as an OutputStream.")))))

(defn ^String as-relative-path
  "Take an as-file-able thing and return a string if it is
   a relative path, else IllegalArgumentException."
  [x]
  (let [^File f (as-file x)]
    (if (.isAbsolute f)
      (throw (js/Error. (str "IllegalArgumentException: " f " is not a relative path")))
      (.getPath f))))


(defn file
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

(defn reader ;InputStream, File, goog.URI, Buffers and String.
  "For all streams it defers back to the stream. Note: stream objects are event driven.
     + buffers => BufferReadStream
     + files + strings => FileInputStream
   + goog.Uri's are treated as local files. No other protocols are supported at this time."
  [x & opts]
  (make-reader x (when opts (apply hash-map opts))))

(defn writer
  "For all streams it defers back to the stream. Note: stream objects are event driven.
   + Files & Strings become FileOutputStreams.
   + goog.Uri's are treated as local files. No other protocols are supported at this time."
  [x & opts]
  (make-writer x (when opts (apply hash-map opts))))

(defn input-stream
  "For all streams it defers back to the stream. Note: stream objects are event driven.
     + buffers => BufferReadStream
     + files + strings => FileInputStream
   + goog.Uri's are treated as local files. No other protocols are supported at this time.
   @return {!IInputStream}"
  [x & opts]
  (make-input-stream x (when opts (apply hash-map opts))))

(defn output-stream
  "For all streams it defers back to the stream. Note: stream objects are event driven.
   + Files & Strings become FileOutputStreams.
   + goog.Uri's are treated as local files. No other protocols are supported at
   this time.
   @return {!IOutputStream}"
  [x & opts]
  (make-output-stream x (when opts (apply hash-map opts))))

(defn ^boolean Buffer?
  "sugar over Buffer.isBuffer
   @return {!boolean}"
  [b]
  (js/Buffer.isBuffer b))

(defn error? [e] (instance? js/Error e))

(defn slurp
  "Returns a string synchronously. Unlike JVM, does not use FileInputStream.
   Only option at this time is :encoding
   If :encoding \"\" (an explicit empty string), returns raw buffer instead of string.
   @return {(string|buffer.Buffer)}"
  [p & opts]
  (let [opts (apply hash-map opts)
        f    (as-file p)]
    (.read f (:encoding opts))))

(defn aslurp
  "@return {!Channel} a which will receive [err data]"
  [p & opts]
  (let [opts (apply hash-map opts)
        f (as-file p)]
    (.aread f (:encoding opts))))

(defn reader-method
  "@param {!string} filepath
   @return {!function(string):Object} appropriate reader based on the file's extension"
  [filepath]
  (condp = (.extname path filepath)
    ".edn"  (fn [contents] (read-string contents))
    ".json" (fn [contents] (js->clj (js/JSON.parse contents) :keywordize-keys true))
    ;xml, csv, transit?
    ;; does it make sense to throw here?
    (throw (js/Error. "sslurp was given an unrecognized file format.
                       The file's extension must be json or edn"))))


(defn sslurp
  "augmented 'super' slurp for convenience. edn|json => clj data-structures
   @returns {!Object} edn"
  [p & opts]
  (let [opts (apply hash-map opts)
        f    (as-file p)
        rdr  (reader-method (.getPath f))]
    (rdr (.read f))))

(defn saslurp
  "augmented 'super' aslurp for convenience. edn|json => clj data-structures put into a ch
   @return {!Channel} which receives [err edn] "
  [p & opts]
  (let [f     (as-file p)
        rdr   (reader-method (.getPath f))
        from  (aslurp f opts)
        to    (chan 1 (map #(if (error? %) % (rdr %))) )
        _     (pipe from to)]
    to))

(defn spit
  "Writes content synchronously to file f.
   :encoding {string} encoding to write the string. Ignored when content is a buffer
   :append - {bool} - if true add content to end of file
   @return {nil} or throws"
  [p content & options]
  (let [opts (apply hash-map options)
        f    (as-file p)]
    (.write f (str content) opts)))

(defn aspit
  "Async spit. Wait for result before writing again!
   @return {!Channel} recieves [err true]"
  [p content & options]
  (let [opts (apply hash-map options)
        f    (as-file p)]
    (.awrite f (str content) opts)))

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
    (fn [f] (.isDirectory (file f) ))
    (fn [d] (map #(.join path d %) (.list (file d))))
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

(defn input-stream?
  "@param {*} obj object to test
   @return {boolean} is object an input-stream?"
  [obj]
  (implements? IInputStream obj))

(defn output-stream?
  "@param {*} obj object to test
   @return {boolean} is object an input-stream?"
  [obj]
  (implements? IOutputStream obj))

(defn stream-type
  "@param {Object} obj The object to test"
  [obj]
  (if (input-stream? obj)
    :InputStream
    (if (output-stream? obj)
      :OutputStream
      false)))

(defn rFile?
  [o]
  (implements? IFile o))

(defmulti
  ^{:doc "Internal helper for copy"
    :private true
    :arglists '([input output opts])}
  do-copy
  (fn [input output opts]
    (if (string? input)
      (recur (as-file input) output opts)
      (if (string? output)
        (recur input (as-file output) opts)
        [(or (stream-type input)  (if (rFile?  input) :File) (type input))
         (or (stream-type output) (if (rFile? output) :File) (type output))]))))


(defmethod do-copy [:InputStream :OutputStream] [input output opts]
  (do (.pipe input output) nil))

(defmethod do-copy [:File :File] [input output opts]
  (let [in  (-> input streams/FileInputStream. )
        out (-> output streams/FileOutputStream. )]
    (do-copy in out opts)))

(defmethod do-copy [:File :OutputStream] [input output opts]
  (let [in (streams/FileInputStream. input)]
    (do-copy in output opts)))

(defmethod do-copy [:InputStream :File] [input output opts]
  (let [out  (streams/FileOutputStream. output)]
    (do-copy input out opts)))

(defmethod do-copy [js/Buffer :OutputStream] [input output opts]
  (do-copy (streams/BufferReadStream. input opts) output opts))

(defmethod do-copy [js/Buffer :File] [input output opts]
  (do-copy (streams/BufferReadStream. input opts) output opts))

(defn copy
  "Copies input to output. Returns nil or throws.
   Input may be an InputStream, File, Buffer, or string.
   Output may be an String, OutputStream or File. 
   Unlike JVM, strings are coerced to files. If you have big chunks of data, use a buffer.
  :encoding = destination encoding to use when copying a Buffer"
  [input output & opts]
  (do-copy input output (when opts (apply hash-map opts))))

(defn -main [& args] nil)
(set! *main-cli-fn* -main)
