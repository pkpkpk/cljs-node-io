(ns cljs-node-io.fs
    (:require [cljs.nodejs :as nodejs :refer [require]]))

(def fs (require "fs"))

(defn chmod
  "@param {string} path
   @param {Number} mode must be an integer"
  [path mode]
  (.chmodSync fs path mode))