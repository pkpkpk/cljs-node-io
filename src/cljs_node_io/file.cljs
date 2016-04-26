(ns cljs-node-io.file "a port of java.io.File's reified files to node"
  (:import goog.Uri)
  (:require [cljs.nodejs :as nodejs :refer [require]]
            [cljs.reader :refer [read-string]]
            [cljs.core.async :as async :refer [put! take! chan <! pipe  alts!]]
            [cljs-node-io.streams :refer [FileInputStream FileOutputStream]]
            [cljs-node-io.fs :as iofs]
            [cljs-node-io.protocols
              :refer [Coercions as-url as-file IFile
                      IOFactory make-reader make-writer make-input-stream make-output-stream]]))

(def fs (require "fs"))
(def path (require "path"))
(def os (require "os"))

(defn setReadable*
  "@param {number} mode : the file's existing mode
   @param {boolean} readable : add or remove read permission
   @param {boolean} ownerOnly : restrict operation to user bit only
   @return {number} A int for chmod that only effects the targeted mode bits"
  [mode readable ownerOnly]
  (condp = [readable ownerOnly]
    [true true]   (bit-or mode 256) ; add-user-read
    [false true]  (bit-and mode (bit-not 256)) ; remove-user-read
    [true false]  (bit-or mode 256 32 4) ; add-read-to-all
    [false false] (bit-and mode  (bit-not 256) (bit-not 32) (bit-not 4)))) ;remove all reads

(defn setReadable
  ([path readable] (setReadable path readable true))
  ([path readable ownerOnly]
   (let [mode (iofs/filemode-int path)
         n    (setReadable* mode readable ownerOnly)]
     (iofs/chmod path n))))

(defn setWritable*
  "@param {number} mode : the file's existing mode
   @param {boolean} writable : add or remove write permission
   @param {boolean} ownerOnly : restrict operation to user bit only
   @return {number} A int for chmod that only effects the targeted mode bits"
  [mode writable ownerOnly]
  (condp = [writable ownerOnly]
    [true true]   (bit-or mode 128) ; add-user-write
    [false true]  (bit-and mode (bit-not 128)) ; remove-user-write
    [true false]  (bit-or mode 128 16 2) ; add-write-to-all
    [false false] (bit-and mode  (bit-not 128) (bit-not 16) (bit-not 2)))) ;remove all writes

(defn setWritable
  ([path writable] (setWritable path writable true))
  ([path writable ownerOnly]
   (let [mode (iofs/filemode-int path)
         n    (setWritable* mode writable ownerOnly)]
     (iofs/chmod path n))))

(defn directory?
  "@param {string} pathstring
   @return {boolean} iff abstract pathname exists and is a directory"
  ^boolean
  [^String pathstring]
  (assert (string? pathstring) "directory? takes a string, perhaps you passed a file instead")
  (let [stats (try (.statSync fs pathstring) (catch js/Error e false))]
    (if stats
      (.isDirectory stats)
      false)))

(defn file?
  "@param {string} pathstring
   @return {boolean} iff abstract pathname exists and is a file"
  ^boolean
  [^String pathstring]
  (assert (string? pathstring) "file? takes a string, perhaps you passed a file instead")
  (let [stats (try (.statSync fs pathstring) (catch js/Error e false))]
    (if stats
      (.isFile stats)
      false)))

(defn get-non-dirs
  "@param {string} pathstring
  returns sequence of strings representing non-existing directory components
   of the passed pathstring, root first, in order "
  [^String pathstring]
  (reverse (take-while #(not (directory? %)) (iterate #(.dirname path %) pathstring))))

(defn ^boolean append? [opts] (boolean (:append opts)))

(defn filepath-dispatch
  ([x] (type x)) ; string || Uri.
  ([x y] (mapv type [x y])))

(defmulti  filepath "signature->File" filepath-dispatch)
(defmethod filepath Uri [u] (.getPath u))
(defmethod filepath js/String  [pathstring] pathstring)
(defmethod filepath [js/String js/String] [parent-str child-str] (str parent-str (.-sep path) child-str))
(defmethod filepath [:File js/String] [parent-file child-str] (str (.getPath parent-file) (.-sep path) child-str))
(defmethod filepath :default [x] (throw (js/Error.
                                         (str "Unrecognized path configuration passed to File constructor."
                                              "\nYou passed " (pr-str x)
                                              "\nYou must pass a [string], [uri], [string string], or [file string].\n" ))))

(defn file-reader
  "Depending on :async? option, this builds an appropriate read method 
   and attaches it to the reified file, returning the passed file.
    - opts {map}: :encoding {string}, :async? {bool}
    - if :async? true, file.read() => channel which receives err|str on successful read
    - if :encoding is \"\" (an explicit empty string), file.read() => raw buffer
   @param {IFile} file to build read method for
   @param {IMap} opts
   @return {IFile} the same file with a read method attached"
  [file opts]
  (if (:async? opts)
    (specify! file Object
      (read [this]
            (let [c (chan) ]
              (.readFile fs (.getPath this) (or (:encoding opts) "utf8") ;if no encoding, returns buffer
                (fn [err data]
                  (put! c (if err err data))))
              c)))
    ;sync reader
    (specify! file Object
      (read [this]
        (.readFileSync fs (.getPath this) (or (:encoding opts) "utf8")))))) ;if no encoding, returns buffer . catch err?

(defn file-writer
  "Depending on the :async? option, this builds an appropriate write method
   and attaches it to the reified file, returning the passed file.
     - opts {map}: :encoding {string}, :append {bool}, :async? {bool}
     - if content is a Buffer instance, opt encoding is ignored
     - if :async? file.write() => channel which receives err|true on successful write.
   @param {IFile} file to build write method for
   @param {IMap} opts a map of options
   @return {IFile} the same file with a write method attached"
  [file opts]
  (if (:async? opts)
    (specify! file Object
      (write [this content]
        (let [filename (.getPath this)
              c (chan)
              cb (fn [err] (put! c (or err true)))]
          (.writeFile fs filename content
                      #js{"flag"     (or (:flags opts) (if (:append opts) "a" "w"))
                          "mode"     (or (:mode opts) 438)
                          "encoding" (or (:encoding opts) "utf8")}
                      cb)
          c)))
    (specify! file Object ;sync
      (write [this content]
        (let [filename (.getPath this)]
          (.writeFileSync fs filename content
                          #js{"flag"     (or (:flags opts) (if (:append opts) "a" "w"))
                              "mode"     (or (:mode opts)  438)
                              "encoding" (or (:encoding opts) "utf8")}))))))



(defn File*
  "@param {string} pathstr The abstract file path
   @return {IFile} a reified file"
  [pathstr]
  (let [pathstring  (atom pathstr)
        f (reify
            IFile
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
            (make-output-stream [^File this opts] (FileOutputStream. this  opts))
            IPrintWithWriter
            (-pr-writer [this writer opts] ;#object[java.io.File 0x751b0a12 "foo\\bar.txt"]
              (-write writer "#object [cljs-node-io.File")
              (-write writer (str "  "  (.getPath this)  " ]")))
            Object
            ; access tests do nothing when pass, throw when fail
            ; https://nodejs.org/api/fs.html#fs_fs_accesssync_path_mode
            (canExecute ^boolean [this]
              (if-not (= "win32" (.-platform js/process))
                (try
                  (if (nil? (.accessSync fs (.getPath this) (.-X_OK fs))) true)
                  (catch js/Error e false))
                (throw (js/Error "Testing if a file is executable has no effect on Windows "))))
            (canRead ^boolean [this]
              (try
                (if (nil? (.accessSync fs (.getPath this) (.-R_OK fs))) true)
                (catch js/Error e false)))
            (canWrite ^boolean [this]
              (try
                (if (nil? (.accessSync fs (.getPath this) (.-W_OK fs))) true)
                (catch js/Error e false)))
            (setReadable [_ r] (setReadable @pathstring r))
            (setReadable [_ r o] (setReadable @pathstring r o))
            (setWritable [_ r] (setWritable @pathstring r))
            (setWritable [_ r o] (setWritable @pathstring r o))
            (createNewFile ^boolean [this]
              (file-writer this {:flags "wx" :async? false})
              (try
                (do
                  (.write this)
                  true)
                (catch js/Error e false)))
            (delete ^boolean [this]
              (if (directory? @pathstring)
                (try
                  (do (.rmdirSync fs @pathstring) true)
                  (catch js/Error e false))
                (try
                   (do (.unlinkSync fs @pathstring) true)
                   (catch js/Error e false))))
            (deleteOnExit [this]
              (.on js/process "exit"  (fn [exit-code] (.delete this))))
            (equals ^boolean [this that] (= this that))
            (exists ^boolean [_]
              (try
                (do
                  (.accessSync fs @pathstring (.-F_OK fs))
                  true)
                (catch js/Error e false)))
            (getAbsoluteFile [this] (as-file (.getAbsolutePath this)))
            (getAbsolutePath [_] (.resolve path @pathstring))
            (getCanonicalFile [this] (as-file (.getCanonicalPath this)))
            (getCanonicalPath [_] (.normalize path @pathstring))
            (getName [_] (.-name (.parse path @pathstring)))
            (getParent [_] (.dirname path @pathstring))
            (getParentFile [this] (as-file (.getParent this))) ;=> File|nil
            (getPath ^string [this] (if (.isAbsolute this) (.getPath (Uri. @pathstring))  @pathstring))
            (hashCode ^int [_] (hash @pathstring))
            (isAbsolute ^boolean [_] (.isAbsolute path @pathstring))
            (isDirectory ^boolean [_] (directory? @pathstring))
            (isFile ^boolean [_] (file? @pathstring))
            (lastModified ^int [_]
              (let [stats (try (.statSync fs @pathstring) (catch js/Error e false))]
                (if stats
                  (.valueOf (.-mtime stats))
                  0)))
            (length ^int [_]
              (let [stats (try (.statSync fs @pathstring) (catch js/Error e false))]
                (if stats
                  (if (.isDirectory stats)
                    nil
                    (.-size stats))
                  0)))
            (list [_] ; ^ Vector|nil
              (if-not (directory? @pathstring)
                nil
                (try
                  (vec (.readdirSync fs @pathstring))
                  (catch js/Error e nil))))
            (list [this filterfn]
              (assert (= 2 (.-length filterfn)) "the file filterfn must accept 2 args, the dir File & name string")
              (if-let [files (.list this)]
                (filterv (partial filterfn @pathstring) files)))
            (listFiles [this]
              (.list this (fn [d name] (file? (str d (.-sep path)  name)))))
            (listFiles [this filterfn]
              (assert (= 2 (.-length filterfn)) "the file filterfn must accept 2 args, the dir File & name string")
              (if-let [files (.listFiles this)]
                (filterv (partial filterfn @pathstring) files)))
            (mkdir ^boolean [_]
              (try
                (do
                  (.mkdirSync fs @pathstring)
                  true)
                (catch js/Error e false)))
            (mkdirs ^boolean [this]
              (let [p  (.getPath this)
                    dirs (get-non-dirs p)]
                (try
                  (do
                    (doseq [d dirs]
                      (.mkdirSync fs d))
                    true)
                  (catch js/Error e false))))
            (renameTo ^boolean [this dest]
              (assert (string? dest) "destination must be a string")
              (try
                (do
                  (.renameSync fs @pathstring dest)
                  (reset! pathstring dest)
                  true)
                (catch js/Error e false)))
            (toString [_]  @pathstring)
            (toURI [f] (Uri. (str "file:" @pathstring))))]
(set! (.-constructor f) :File)
f))


(defn File
  "@return {IFile} a reified file"
  ([a]   (File*  (filepath a)))
  ([a b] (File*  (filepath a b))))

(defn createTempFile
  "@return {IFile} a reified file"
  ([prefix] (createTempFile prefix nil nil))
  ([prefix suffix] (createTempFile prefix suffix nil))
  ([prefix suffix dir]
    (let [tmpd (or dir (.tmpdir os))
          path (str tmpd (.-sep path) prefix (or suffix ".tmp"))
          f    (File. path)]
      f)))
