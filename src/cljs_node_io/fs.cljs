(ns cljs-node-io.fs
    (:require-macros [cljs-node-io.macros :refer [try-true]])
    (:require [cljs.nodejs :as nodejs :refer [require]]))

(def fs (require "fs"))
(def path (js/require "path"))

(defn stat 
  [path]
  (.statSync fs path))

(defn to-bit [number] (if-not (zero? number) 1 0))

(defn bita->int
  "takes an array of 1s an 0s and converts it to an integer"
  [bita]
  (js/parseInt (.join bita "") 2))

(defn filemode [path]
  (let [s (.statSync fs path)
        mode (aget s "mode")
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

(defn filemode-int [p] (bita->int (filemode p)))

(defn chmod
  "@param {string} path
   @param {Number} mode must be an integer"
  [path mode]
  (.chmodSync fs path mode))

(defn chown
  "@param {string} path
   @param {Number} uid
   @param {Number} gid"
  [path uid gid]
  (.chownSync fs path uid gid))

(defn gid-uid
  "@return {IMap}"
  []
  {:gid (.getgid js/process) :uid (.getuid js/process)})

(defn utimes
  [path atime mtime]
  (.utimesSync fs path atime mtime))

(defn hidden?
  "@param {string} pathstr
   @return {boolean} is the file hidden (unix only)"
  [pathstr]
  (.test (js/RegExp. "(^|\\/)\\.[^\\/\\.]" ) pathstr))

(defn dir?
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

(defn absolute?
  "@param {string} p : path to test
   @return {boolean} is p an absolute path"
  [p]
  (.isAbsolute path p))

(defn fexists?
  "test if a file exists
   @param {string} p : file path to test
   @return {boolean}"
  [p]
  (try-true (.accessSync fs p (.-F_OK fs))))

(defn readable?
  "@param {string} p path to test for process read permission
   @return {boolean}"
  [p]
  (try-true (.accessSync fs p (.-R_OK fs))))

(defn writable?
  "@param {string} p path to test for process write permission
   @return {boolean}"
  [p]
  (try-true (.accessSync fs p (.-W_OK fs))))

(defn executable?
  "@param {string} p path to test for process executable permission
   @return {boolean}"
  [p]
  (if-not (= "win32" (.-platform js/process))
    (try-true (.accessSync fs p (.-X_OK fs)))
    (throw (js/Error "Testing if a file is executable has no effect on Windows "))))

(defn mkdir
  "@param {string} p : path of directory to create"
  [pathstring]
  (.mkdirSync fs pathstring))

(defn rmdir
  "@param {string} p path of directory to remove"
  [p]
  (try-true (.rmdirSync fs p)))

(defn unlink
  [p]
  (try-true (.unlinkSync fs p)))

(defn delete
  "@param {string} pathstring
   @return {boolean}"
  [pathstring]
  (if (dir? pathstring)
    (rmdir pathstring)
    (unlink pathstring)))

(defn rename
  "@param {string} prevp : existing file path
   @param {string} newp  : new file path"
  [prevp newp]
  (.renameSync fs prevp newp))

(defn readdir
  "@param {string} dirpath : directory path to read
   @return {IVector} vector of strings representing the directory contents"
  [dirpath]
  (vec (.readdirSync fs dirpath)))

(defn dirname
  "@param {string} p : path to get parent of
   @return {string} the parent directory"
  [p]
  (.dirname path p))
