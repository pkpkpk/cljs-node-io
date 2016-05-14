(ns cljs-node-io.fs
  (:require-macros [cljs-node-io.macros :refer [try-true]])
  (:require [cljs.core.async :as async :refer [put! take! chan <! pipe  alts!]]))

(def fs (js/require "fs"))
(def path (js/require "path"))
(def os (js/require "os"))

(def tmpdir (.tmpdir os))
(def sep (.-sep path))

(defn stat
  "@param {!string} pathstring
   @return {!fs.Stats} file stats object"
  [pathstring]
  (.statSync fs pathstring))

(defn to-bit [number] (if-not (zero? number) 1 0))

(defn bita->int
  "@param {!Array<!Number>} bita : an array of 1s an 0s
   @return {!Number} integer"
  [bita]
  (js/parseInt (.join bita "") 2))

(defn stat->perm-bita
  "@param {!fs.Stats} s : a fs.Stats object
   @return {!Array<Number>}"
  [s]
  (let [mode (aget s "mode")
        ownr (bit-and mode 256)
        ownw (bit-and mode 128)
        ownx (bit-and mode 64)
        grpr (bit-and mode 32)
        grpw (bit-and mode 16)
        grpx (bit-and mode 8)
        othr (bit-and mode 4)
        othw (bit-and mode 2)
        othx (bit-and mode 1)
        a #js [ownr ownw ownx grpr grpw grpx othr othw othx]]
    (amap a i res (to-bit (aget a i)))))

(defn permissions
  "@param {!string} filepath
   @return {!Number}"
  [filepath]
  (-> (stat filepath) stat->perm-bita bita->int))

(defn chmod
  "@param {!string} path
   @param {!Number} mode must be an integer"
  [path mode]
  (.chmodSync fs path mode))

(defn chown
  "@param {!string} path
   @param {!Number} uid
   @param {!Number} gid"
  [path uid gid]
  (.chownSync fs path uid gid))

(defn gid-uid
  "@return {!IMap}"
  []
  {:gid (.getgid js/process) :uid (.getuid js/process)})

(defn utimes
  [path atime mtime]
  (.utimesSync fs path atime mtime))

(defn hidden?
  "@param {!string} pathstr
   @return {!boolean} is the file hidden (unix only)"
  [pathstr]
  (.test (js/RegExp. "(^|\\/)\\.[^\\/\\.]" ) pathstr))

(defn dir?
  "@param {!string} pathstring
   @return {!boolean} iff abstract pathname exists and is a directory"
  ^boolean
  [^String pathstring]
  (assert (string? pathstring) "directory? takes a string, perhaps you passed a file instead")
  (let [stats (try (.statSync fs pathstring) (catch js/Error e false))]
    (if stats
      (.isDirectory stats)
      false)))

(defn file?
  "@param {!string} pathstring
   @return {!boolean} iff abstract pathname exists and is a file"
  ^boolean
  [^String pathstring]
  (assert (string? pathstring) "file? takes a string, perhaps you passed a file instead")
  (let [stats (try (.statSync fs pathstring) (catch js/Error e false))]
    (if stats
      (.isFile stats)
      false)))

(defn absolute?
  "@param {!string} p : path to test
   @return {!boolean} is p an absolute path"
  [p]
  (.isAbsolute path p))

(defn fexists?
  "test if a file exists
   @param {!string} p : file path to test
   @return {!boolean}"
  [p]
  (try-true (.accessSync fs p (.-F_OK fs))))

(defn readable?
  "@param {!string} p path to test for process read permission
   @return {!boolean}"
  [p]
  (try-true (.accessSync fs p (.-R_OK fs))))

(defn writable?
  "@param {!string} p path to test for process write permission
   @return {!boolean}"
  [p]
  (try-true (.accessSync fs p (.-W_OK fs))))

(defn executable?
  "@param {!string} p path to test for process executable permission
   @return {!boolean}"
  [p]
  (if-not (= "win32" (.-platform js/process))
    (try-true (.accessSync fs p (.-X_OK fs)))
    (throw (js/Error "Testing if a file is executable has no effect on Windows "))))

(defn mkdir
  "@param {!string} pathstring : path of directory to create"
  [pathstring]
  (.mkdirSync fs pathstring))

(defn rmdir
  "@param {!string} pathstring : path of directory to remove"
  [pathstring]
  (try-true (.rmdirSync fs pathstring)))

(defn unlink
  "@param {!string} pathstring : path of file to unlink
   @return {!boolean} whether the op succeeded"
  [pathstring]
  (try-true (.unlinkSync fs pathstring)))

(defn delete
  "@param {!string} pathstring : can be file or directory
   @return {!boolean} whether the op succeeded"
  [pathstring]
  (if (dir? pathstring)
    (rmdir pathstring)
    (unlink pathstring)))

(defn rename
  "@param {!string} prevp : existing file path
   @param {!string} newp  : new file path"
  [prevp newp]
  (.renameSync fs prevp newp))

(defn readdir
  "@param {!string} dirpath : directory path to read
   @return {!IVector} vector of strings representing the directory contents"
  [dirpath]
  (vec (.readdirSync fs dirpath)))

(defn dirname
  "@param {!string} pathstring : path to get parent of
   @return {!string} the parent directory"
  [pathstring]
  (.dirname path pathstring))

(defn filename
  "@return {!string}"
  ([p] (.basename path p))
  ([p ext] (.basename path p ext)))

(defn resolve-path
  "@param {!string} pathstring : pathstring to resolve to absolute path
   @return {!string}"
  [pathstring]
  (.resolve path pathstring)) ; this should dispatch on type, fs.resolve has multiple arities

(defn normalize-path
  "@param {!string} pathstring : pathstring to normalize
   @return {!string}"
  [pathstring]
  (.normalize path pathstring))

(defn ext
  "@param {string} pathstring : file to get extension from
   @return {string}"
  [pathstring]  (.extname path pathstring))


(defn readFile
  "if :encoding is \"\" (an explicit empty string) => raw buffer"
  ([pathstring] (readFile pathstring "utf8"))
  ([pathstring enc] (.readFileSync fs pathstring enc)))

(defn areadFile
  "if :encoding is \"\" (an explicit empty string) => raw buffer
   => channel which receives err|str on successful read"
  ([p](areadFile p "utf8"))
  ([p enc]
    (let [c (chan)]
      (.readFile fs p enc (fn [err data] (put! c (if err err data))))
      c))
  ([p enc cb] (.readFile fs p enc cb)))

(defn writeFile
  "synchronously writes content to file represented by pathstring.
   @param {!string} pathstring : file to write to
   @param {(string|buffer.Buffer)} content : if buffer, :encoding is ignored
   @param {?IMap} opts : :encoding {string}, :append {bool}, :flags {string}, :mode {int}
    - flags override append"
  [pathstring content opts]
  (.writeFileSync fs pathstring content
                  #js{"flag"     (or (:flags opts) (if (:append opts) "a" "w"))
                      "mode"     (or (:mode opts)  438)
                      "encoding" (or (:encoding opts) "utf8")}))

(defn awriteFile ;add custom cb arity
  "by default returns channel which receives err|true on successful write.
  or pass in custom callback"
  ([pathstring content opts]
   (let [c (chan)
         cb (fn [err] (put! c (or err true)))]
     (.writeFile fs pathstring content
                 #js{"flag"     (or (:flags opts) (if (:append opts) "a" "w"))
                     "mode"     (or (:mode opts) 438)
                     "encoding" (or (:encoding opts) "utf8")}
                 cb)
     c))
  ([pathstring content opts cb]
   (.writeFile fs pathstring content
               #js{"flag"     (or (:flags opts) (if (:append opts) "a" "w"))
                   "mode"     (or (:mode opts) 438)
                   "encoding" (or (:encoding opts) "utf8")}
               cb)))