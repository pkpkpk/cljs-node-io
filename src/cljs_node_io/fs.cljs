(ns cljs-node-io.fs
    (:require [cljs.nodejs :as nodejs :refer [require]]))

(def fs (require "fs"))

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