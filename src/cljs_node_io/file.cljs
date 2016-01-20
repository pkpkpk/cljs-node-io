(ns cljs-node-io.file "a bunch of nonsense for mocking java.io.File's polymorphic constructor"
  (:import goog.Uri)
  (:require [cljs.nodejs :as nodejs :refer [require]]
            [cljs-node-io.streams :refer [FileInputStream FileOutputStream]]
            [cljs-node-io.protocols
              :refer [IGetType get-type
                      Coercions as-url as-file
                      IOFactory make-reader make-writer make-input-stream make-output-stream]]))

(def fs (require "fs"))
(def path (require "path"))
(def sep (.-sep path))
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

(extend-type Uri
  IGetType
  (get-type [u] :uri))

(extend-type string
  IGetType
  (get-type [u] :string))

(defn ^Boolean append? [opts] (boolean (:append opts)))

(defn filepath-dispatch
  ([x] (try (get-type x) (catch js/Object e :default)) ) ; string || Uri.
  ([x y] ; [string string]  || [File string]
   (case (try (mapv get-type [x y]) (catch js/Object e nil)) ;pre post instead?
     [:string :string] :string-string
     [:file :string] :file-string
     :default)))

(defmulti  filepath "signature->File" filepath-dispatch)
(defmethod filepath :uri [u] (.getPath u))
(defmethod filepath :string  [pathstring] pathstring)
(defmethod filepath :string-string [parent-str child-str] (str parent-str "/" child-str))
(defmethod filepath :file-string [parent-file child-str] (str (.getPath parent-file) "/" child-str))
(defmethod filepath :default [x] (throw (js/Error.
                                         (str "Unrecognized path configuration passed to File constructor."
                                              "\nYou passed " (pr-str x)
                                              "\nYou must pass a [string], [uri], [string string], or [file string].\n" ))))

(defn file-stream-reader [filestream opts]
  (make-reader filestream opts)) ;just defering to file stream object for now

(defn file-stream-writer [filestream opts]
  (make-writer filestream opts)) ;just defering to file stream object for now

(defn file-sync-writer [file opts]
  (reify Object
    (write [this content]
      (let [filename (.getPath file)]
        (if (:append? opts) ;should check encoding, mode, sync too?
          (.writeFileSync fs filename content  #js{"flag" "a"})
          (.writeFileSync fs filename content  #js{"flag" "w"}))))))



(defn file-reader [f opts]
  (if (:stream? opts)
    (file-stream-reader (make-input-stream f opts) opts)
    (.readFileSync fs (.getPath f) (or (:encoding opts) "utf8")))) ;hide inside a .read method?

(defn file-writer [f opts]
  (if (:stream? opts)
    (file-stream-writer (make-output-stream f opts) opts)
    (file-sync-writer f opts)))



(defn File* [pathstring]
  (reify
    IEquiv
    (-equiv [this that]
      (let [pathntype (juxt #(.-getPath %) get-type)]
        (= (pathntype this) (pathntype that))))
    IGetType
    (get-type [f] :file)
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
  ([a] (File*  (filepath a)))
  ([a b] (File*  (filepath a b))))

(defn temp-file
  ([prefix suffix] (temp-file prefix suffix nil))
  ([prefix suffix content]
    (let [tmpd (.tmpdir os)
          path (str tmpd (.-sep path) prefix "." suffix)
          f    (File. path)
          _    (.deleteOnExit f)]
      f)))
