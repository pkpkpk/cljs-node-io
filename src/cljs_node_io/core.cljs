(ns cljs-node-io.core
  (:require [cljs.nodejs :as nodejs :refer [require]]
            [cljs-node-io.file :refer [File]]
            ; [cljs-node-io.reader :refer [reader]]
            [cljs-node-io.streams :refer [FileInputStream]]
            [cljs-node-io.protocols
              :refer [get-type
                      Coercions as-url as-file
                      IOFactory make-reader make-writer make-input-stream make-output-stream]]
            [clojure.string :as st]
            [goog.string :as gstr])
  (:import goog.Uri
           [goog.string StringBuffer] )
  )

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
  (or (:encoding opts) "utf8")) ;<=== utf8?

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
  (make-reader [x opts] (make-reader (File. x) opts)); choice to make stream is handled by opts passed to reader
  (make-writer [x opts] (make-writer (File. x) opts))
  (make-input-stream [^String x opts](try
                                        (make-input-stream (Uri. x) opts)
                                        (catch js/Error e ;MalformedURLException
                                          ; (println "malformed URL string, trying string as file...")
                                          (make-input-stream (File. x) opts))))
  (make-output-stream [^String x opts] (try
                                        (make-output-stream (Uri. x) opts)
                                          (catch js/Error err ;MalformedURLException
                                              (make-output-stream (File. x) opts)))))






(defn -main [& args] nil)
(set! *main-cli-fn* -main)
