(ns cljs-node-io.fs
    (:require [cljs.nodejs :as nodejs :refer [require]]))

(def fs (require "fs"))

(defn to-bit [number] (if-not (zero? number) 1 0))

(defn to-int
  "takes a vector of 1s an 0s and converts it to an integer"
  [bitv]
  (assert (vector? bitv))
  (assert (every? number? bitv))
  (let [bb (.join (clj->js bitv) "")
        i  (js/parseInt bb 2)]
    i))

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
        othx (bit-and mode 1)]
    (mapv to-bit [ownr ownw ownx grpr grpw grpx othr othw othx])))

(defn filemode-int [p] (to-int (filemode p)))

(defn chmod
  "@param {string} path
   @param {Number} mode must be an integer"
  [path mode]
  (.chmodSync fs path mode))

