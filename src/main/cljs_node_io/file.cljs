(ns cljs-node-io.file "a port of java.io.File's reified files to node"
  (:import goog.Uri)
  (:require-macros [cljs-node-io.macros :refer [try-true]])
  (:require [cljs-node-io.streams :refer [FileInputStream FileOutputStream]]
            [cljs-node-io.fs :as iofs]
            [cljs-node-io.protocols
              :refer [Coercions as-url as-file IFile
                      IOFactory make-reader make-writer make-input-stream make-output-stream]]))

(defn setReadable*
  "@param {!number} mode : the file's existing mode
   @param {!boolean} readable : add or remove read permission
   @param {!boolean} ownerOnly : restrict operation to user bit only
   @return {!number} A int for chmod that only effects the targeted mode bits"
  [mode ^boolean readable ^boolean ownerOnly]
  (condp = [readable ownerOnly]
    [true true]   (bit-or mode 256) ; add-user-read
    [false true]  (bit-and mode (bit-not 256)) ; remove-user-read
    [true false]  (bit-or mode 256 32 4) ; add-read-to-all
    [false false] (bit-and mode  (bit-not 256) (bit-not 32) (bit-not 4)))) ;remove all reads

(defn setReadable
  "toggles the readable permission bit(s) for the specified filepath.
   If readable, set 1 else 0
   If ownerOnly (default) set just user, else set for group & other as well.
   Does not affect other permission bits."
  ([pathstr readable] (setReadable pathstr readable true))
  ([pathstr ^boolean readable ^boolean ownerOnly]
   (let [mode (iofs/permissions (iofs/stat pathstr))
         n    (setReadable* mode readable ownerOnly)]
     (iofs/chmod pathstr n))))

(defn setWritable*
  "@param {!number} mode : the file's existing mode
   @param {!boolean} writable : add or remove write permission
   @param {!boolean} ownerOnly : restrict operation to user bit only
   @return {!number} A int for chmod that only effects the targeted mode bits"
  [mode ^boolean writable ^boolean ownerOnly]
  (condp = [writable ownerOnly]
    [true true]   (bit-or mode 128) ; add-user-write
    [false true]  (bit-and mode (bit-not 128)) ; remove-user-write
    [true false]  (bit-or mode 128 16 2) ; add-write-to-all
    [false false] (bit-and mode  (bit-not 128) (bit-not 16) (bit-not 2)))) ;remove all writes

(defn setWritable
  "toggles the writable permission bit(s) for the specified filepath.
   If writable, set 1 else 0
   If ownerOnly (default) set just user, else set for group & other as well.
   Does not affect other permission bits."
  ([pathstr ^boolean writable] (setWritable pathstr writable true))
  ([pathstr ^boolean writable ^boolean ownerOnly]
   (let [mode (iofs/permissions (iofs/stat pathstr))
         n    (setWritable* mode writable ownerOnly)]
     (iofs/chmod pathstr n))))

(defn setExecutable*
  "@param {!number} mode : the file's existing mode
   @param {!boolean} executable : add or remove execute permission
   @param {!boolean} ownerOnly : restrict operation to user bit only
   @return {!number} A int for chmod that only effects the targeted mode bits"
  [mode ^boolean executable ^boolean ownerOnly]
  (condp = [executable ownerOnly]
    [true true]   (bit-or mode 64) ; add-user-execute
    [false true]  (bit-and mode (bit-not 64)) ; remove-user-execute
    [true false]  (bit-or mode 64 8 1) ; add-execute-to-all
    [false false] (bit-and mode  (bit-not 64) (bit-not 8) (bit-not 1)))) ;remove all executes

(defn setExecutable
  "toggles the executable permission bit(s) for the specified filepath.
   If executable, set 1 else 0
   If ownerOnly (default) set just user, else set for group & other as well.
   Does not affect other permission bits."
  ([pathstr ^boolean executable] (setExecutable pathstr executable true))
  ([pathstr ^boolean executable ^boolean ownerOnly]
   (let [mode (iofs/permissions (iofs/stat pathstr))
         n    (setExecutable* mode executable ownerOnly)]
     (iofs/chmod pathstr n))))

(defn get-non-dirs
  "Returns sequence of strings representing non-existing directory components
   of the passed pathstring, root first, in order
   @param {!string} pathstring
   @return {!ISeq}"
  [^String pathstring]
  (reverse (take-while (complement iofs/dir?) (iterate iofs/dirname pathstring))))

(deftype File
  [^:mutable pathstring]
  IFile
  IEquiv
  (-equiv [this that]
    (let [pathntype (juxt #(.-getPath %) type)]
      (= (pathntype this) (pathntype that))))
  Coercions
  (as-file [f] f)
  (as-url [f] (.to-url f))
  IOFactory
  (make-reader [this opts] (make-reader (make-input-stream  this opts) opts))
  (make-writer [this opts] (make-writer (make-output-stream this opts) opts))
  (make-input-stream [this opts] (FileInputStream this opts))
  (make-output-stream [this opts] (FileOutputStream this  opts))
  IPrintWithWriter
  (-pr-writer [this writer opts] ;#object[java.io.File 0x751b0a12 "foo\\bar.txt"]
    (-write writer "#object [cljs-node-io.File")
    (-write writer (str "  "  (.getPath this)  " ]")))
  Object
  (read [this](iofs/readFile pathstring "utf8"))
  (read [this enc](iofs/readFile pathstring enc))
  (aread [this](iofs/areadFile pathstring "utf8"))
  (aread [this enc](iofs/areadFile pathstring enc))
  (write [this content opts] (iofs/writeFile pathstring content opts))
  (awrite [this content opts] (iofs/awriteFile pathstring content opts))
  (canRead ^boolean [this] (iofs/readable? pathstring)) ;untested
  (canWrite ^boolean [this] (iofs/writable? pathstring)) ;untested
  (canExecute ^boolean [this] (iofs/executable? pathstring)) ;untested
  (setReadable [_ r] (setReadable pathstring r))
  (setReadable [_ r o] (setReadable pathstring r o))
  (setWritable [_ w] (setWritable pathstring w))
  (setWritable [_ w o] (setWritable pathstring w o))
  (setExecutable [_ e] (setExecutable pathstring e))
  (setExecutable [_ e o] (setExecutable pathstring e o))
  (setReadOnly [this] (.setWritable this false false))
  (setLastModified [_ time] (iofs/utimes pathstring time time)) ;sets atime + mtime
  (createNewFile ^boolean [this] (try-true (.write this "" {:flags "wx"})))
  (delete ^boolean [this] (try-true (iofs/rm pathstring)))
  (deleteOnExit [this]
    (.on js/process "exit"  (fn [exit-code] (.delete this))))
  (equals ^boolean [this that] (= this that))
  (exists ^boolean [_](iofs/fexists? pathstring))
  (getAbsoluteFile [this] (File. (.getAbsolutePath this)))
  (getAbsolutePath [_] (iofs/realpath pathstring))
  (getCanonicalFile [this] (File. (.getCanonicalPath this)))
  (getCanonicalPath [_] (iofs/normalize-path pathstring))
  (getName [_] (iofs/basename pathstring))
  (getExt  [_] (iofs/ext pathstring))
  (getParent [_] (iofs/dirname pathstring))
  (getParentFile [this] (File. (.getParent this))) ;=> File|nil
  (getPath [_] pathstring)
  (hashCode ^int [_] (hash pathstring))
  (isAbsolute ^boolean [_] (iofs/absolute? pathstring))
  (isDirectory ^boolean [_] (iofs/dir? pathstring))
  (isFile ^boolean [_] (iofs/file? pathstring))
  (isHidden ^boolean [_](iofs/hidden? pathstring))
  (lastModified ^int [_]
    (let [stats (try (iofs/stat pathstring) (catch js/Error e false))]
      (if stats
        (.valueOf (.-mtime stats))
        0)))
  (length ^int [_]
    (let [stats (try (iofs/stat pathstring) (catch js/Error e false))]
      (if stats
        (if (.isDirectory stats)
          nil
          (.-size stats))
        0)))
  (list [_] ; ^ Vector|nil
    (if-not (iofs/dir? pathstring)
      nil
      (try
        (iofs/readdir pathstring)
        (catch js/Error e nil))))
  (list [this filterfn]
    (if-let [files (.list this)]
      (filterv (partial filterfn pathstring) files)))
  (listFiles [this]
    (mapv  #(File. (str pathstring iofs/sep %)) (.list this)))
  (listFiles [this filterfn]
    (if-let [files (.listFiles this)]
      (filterv (partial filterfn pathstring) files))) ;trandsucers?
  (mkdir ^boolean [_](try-true (iofs/mkdir pathstring)))
  (mkdirs ^boolean [this]
    (let [p  (.getPath this)
          dirs (get-non-dirs p)]
      (try-true (doseq [d dirs] (iofs/mkdir d)))))
  (renameTo ^boolean [this dest]
    (assert (string? dest) "destination must be a string")
    (try-true
      (iofs/rename pathstring dest)
      (iofs/unlink pathstring)
      (set! pathstring dest)))
  (stats [_] (iofs/stat->clj (iofs/stat pathstring)))
  (toString [_]  pathstring)
  (toURI [f] (Uri. (str "file:" pathstring))))

