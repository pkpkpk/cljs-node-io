(ns cljs-node-io.file "a bunch of nonsense for mocking java.io.File's polymorphic constructor"
  (:import goog.Uri)
  (:require [cljs.nodejs :as nodejs :refer [require]]
            [cljs.core.async :as async :refer [put! take! chan <! pipe  alts!]]
            [cljs-node-io.streams :refer [FileInputStream FileOutputStream]]
            [cljs-node-io.protocols
              :refer [Coercions as-url as-file
                      IOFactory make-reader make-writer make-input-stream make-output-stream]]))

(def fs (require "fs"))
(def path (require "path"))
(def os (require "os"))


(defn directory?
  "true iff file denoted by this abstract pathname exists and is a directory"
  ^boolean
  [^String pathstring]
  (let [stats (try (.statSync fs pathstring) (catch js/Error e false))]
    (if stats
      (.isDirectory stats)
      false)))

(defn get-dirs
  "returns sequence of strings representing non-existing directory components
   of the passed pathstring, root first, in order "
  [^String pathstring]
  (reverse (take-while #(not (directory? %)) (iterate #(.dirname path %) pathstring))))

(defn has-ext? ^boolean [^String pathstring] (not= "" (.extname path pathstring)))

(defn ^Boolean append? [opts] (boolean (:append opts)))

(defn filepath-dispatch
  ([x] (type x)) ; string || Uri.
  ([x y] (mapv type [x y])))

(defmulti  filepath "signature->File" filepath-dispatch)
(defmethod filepath Uri [u] (.getPath u))
(defmethod filepath js/String  [pathstring] pathstring)
(defmethod filepath [js/String js/String] [parent-str child-str] (str parent-str "/" child-str))
(defmethod filepath [:File js/String] [parent-file child-str] (str (.getPath parent-file) "/" child-str))
(defmethod filepath :default [x] (throw (js/Error.
                                         (str "Unrecognized path configuration passed to File constructor."
                                              "\nYou passed " (pr-str x)
                                              "\nYou must pass a [string], [uri], [string string], or [file string].\n" ))))

(defn file-stream-reader [filestream opts]
  (make-reader filestream opts)) ;just defering to file stream object for now

(defn file-stream-writer [filestream opts]
  (make-writer filestream opts)) ;just defering to file stream object for now

(defn file-reader [f opts]
  ;hide inside a .read method? channel options?
  (if (:stream? opts)
    (file-stream-reader (make-input-stream f opts) opts)
    (if (:async? opts)
      (let [c (chan) ]
        (.readFile fs (.getPath f) (or (:encoding opts) "utf8") ;if no encoding, returns buffer
          (fn [err data] (put! c (or err data))))
        c)
      (.readFileSync fs (.getPath f) (or (:encoding opts) "utf8"))))) ;if no encoding, returns buffer . catch err?




(defn file-writer
  "Builds an appropriate write method given opts and attaches it to the reified file.
    - TODO: support flags in opts?
    - encoding, append?, async?, stream?
    - if content is a Buffer instance, opt encoding is ignored
    - if :async? true, returns a chan which recieves err|true on success"
  [file opts]
  (if (:stream? opts) ;async write option too
    (file-stream-writer (make-output-stream file opts) opts)
    (if (:async? opts)
      (specify! file Object
        (write [this content]
          (let [filename (.getPath this)
                c (chan)
                cb (fn [err] (put! c (or err true)))] ;mode? more flags?
            (.writeFile fs filename content  #js{"flag" (if (:append? opts) "a" "w")
                                                 "encoding" (or (:encoding opts) "utf8")} cb)
            c)))
      (specify! file Object ;sync
        (write [this content]
          (let [filename (.getPath this)] ;mode? more flags?
            (.writeFileSync fs filename content
                            #js{"flag"     (if (:append? opts) "a" "w")
                                "encoding" (or (:encoding opts) "utf8")})))))))



(defn File* [pathstring]
  (reify
    IEquiv
    (-equiv [this that]
      (let [pathntype (juxt #(.-getPath %) type)]
        (= (pathntype this) (pathntype that))))
    Coercions
    (as-file [f] f)
    (as-url [f] (.to-url f))
    IOFactory
    (make-reader [^File this opts] (file-reader this opts))
    (make-writer [^File this opts] (file-writer this opts))
    (make-input-stream [^File this opts] (FileInputStream. this opts))
    (make-output-stream [^File this opts] (FileOutputStream. this (append? opts)) opts);?????????
    Object
    (delete [this] (try
                     (do
                       (.unlinkSync fs pathstring)
                       true)
                     (catch js/Error e false)))
    (deleteOnExit [this] (.on js/process "exit" #(.delete this)))
    (exists [this] (.existsSync fs pathstring)) ;deprecated buts stats docs are shit so im using it
    (toURI [f] (Uri. pathstring))
    (getAbsolutePath [f] (.resolve path pathstring))
    (getPath [f] (if (.isAbsolute  f) (.getPath (Uri. pathstring))  pathstring))
    (isDirectory [_] (directory? pathstring)) ;=> Boolean
    (isAbsolute [_] (.isAbsolute path pathstring)) ;=>Boolean
    (getParentFile [_] (as-file (.dirname path pathstring))) ;=> File|nil
    (mkdirs [this] ; Creates the directory named by this abstract pathname, including any necessary but nonexistent parent directories.
      (let [p  (.getPath this)
            dirs (get-dirs p)]
        (try
          (do
            (doseq [d dirs]
              (if (not (directory? d))
                (.mkdirSync fs d)))
            true); true iff the directory was created, along with all necessary parent directories;
          (catch js/Error e false)))))) ; if false it may have succeeded in creating some of the necessary parent directories


(defn File
  ([a]
   (let [f  (File*  (filepath a))]
     (set! (.-constructor f) :File)
     f))
  ([a b]
   (let [f (File*  (filepath a b))]
     (set! (.-constructor f) :File)
     f)))

(defn temp-file
  ([prefix suffix] (temp-file prefix suffix nil))
  ([prefix suffix content]
    (let [tmpd (.tmpdir os)
          path (str tmpd (.-sep path) prefix "." suffix)
          f    (File. path)
          _    (.deleteOnExit f)]
      f)))
