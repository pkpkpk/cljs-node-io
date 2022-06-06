(ns cljs-node-io.async-test-helpers
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.test :refer [deftest is testing run-tests are use-fixtures async]]))

(def stream (js/require "stream"))
(def EventEmitter (.-EventEmitter (js/require "events")))

(defn emit [this evkw val]
  (.apply (.-emit this) this (into-array (cons (name evkw) val))))

(defn mock-stream [xf]
  (.setEncoding (new stream.Transform #js {"writableObjectMode" true "transform" xf}) "utf8"))

(defn readable-test-xf
  "[:end nil] will kill, emit 'end'
   [:data [v]] will buffer data, emit 'data'
   [:close [v]] will kill, emit 'end' & 'close'"
  [[ev val] encoding cb]
  (this-as this
    (case ev
      :data  (.push this (val 0))
      :end   (.push this nil) ;end of resource, kill stream
      :close (do (.push this nil) (emit this ev val))
      (emit this ev val))
    (cb)))

(def mock-readable-inputs
  [[:data ["a string"]]
   [:error [(js/Error. "some error")]]])

(defn writable-test-xf
  "strings are buffered, use to induce backpressure
   [:close [:a :b]] will trigger 'end' & 'close' events"
  [data encoding cb]
  (this-as this
    (if (string? data)
      (.push this data)
      (if (vector? data)
        (let [[ev val] data]
          (if (= ev :close) (.end this))
          (emit this ev val))))
    (cb)))
