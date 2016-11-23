(ns cljs-node-io.proc
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as casync :refer [put! take! chan <! pipe alts! promise-chan]]
            [cljs.core.async.impl.protocols :as impl]
            [cljs-node-io.async :refer [cp->ch]]
            [cljs-node-io.protocols :refer [IChildProcess]]
            [clojure.string :as string :refer [split-lines]]))

(def childproc (js/require "child_process"))

(defn exec
  "@return {(buffer.Buffer|String)}"
  ([cmdstr](exec cmdstr nil))
  ([cmdstr opts]
    (childproc.execSync cmdstr (clj->js opts))))

(defn aexec
  "@return {!impl/Channel} <= [Error {string|Buffer} {string|Buffer}]"
  ([cmdstr](aexec cmdstr nil))
  ([cmdstr opts]
   (let [out (promise-chan)
         cb (fn [err stdout stderr]
              (put! out [err stdout stderr]))]
     (childproc.exec cmdstr (clj->js opts) cb)
     out)))

(defn execFile
  "@param {!string} pathstr :: the file to execute
   @param {!IVector} args :: args to the executable
   @param {!IMap} opts :: execution options
   @return {(buffer.Buffer|String)}"
  [pathstr args opts]
  (childproc.execFileSync pathstr (into-array args) (clj->js opts)))

(defn aexecFile
  "@param {!string} pathstr :: the file to execute
   @param {!IVector} args :: args to the executable
   @param {!IMap} opts :: execution options
   @return {!impl/Channel} <= [Error {string|Buffer} {string|Buffer}]"
  [pathstr args opts]
  (let [out (promise-chan)
        cb (fn [err stdout stderr] (put! out [err stdout stderr]))]
    (childproc.execFile pathstr (into-array args) (clj->js opts) cb)
    out))

(defn spawn
  "@param {!string} cmd :: command to execute in a shell
   @param {!IVector} args :: args to the shell command
   @param {!IMap} opts :: execution options
   @return {!child_process.ChildProcess}"
  [cmd args opts]
  (let [opts (clj->js opts)
        proc (childproc.spawn cmd (into-array args) opts)]
    proc))

(defn spawn-sync
  "An exception to the 'a' prefix rule: cp.spawnSync will block until its
   process exits before returning a modified ChildProcess object. This is
   significantly less useful than a persisting asynchronous spawn
   @param {!string} cmd :: command to execute in a shell
   @param {!IVector} args :: args to the shell command
   @param {!IMap} opts :: map of execution options
   @return {!Object}"
  [cmd args opts]
  (let [opts (clj->js opts)
        proc (childproc.spawnSync cmd (into-array args) opts)]
    proc))

(defn fork
  "@param {!string} modulePath :: path to js file to run
   @param {!IVector} args :: arguments to the js file
   @param {!IMap} opts :: map of execution options
   @return {!child_process.ChildProcess}"
  [modulePath args opts]
  (let [args (apply array args)
        opts (merge {:silent true :stdio "pipe"} opts)
        ps (childproc.fork modulePath args (clj->js opts))]
    (.setEncoding (.-stdout ps) "utf8")
    (.setEncoding (.-stderr ps) "utf8")
    ps))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- cp-send
  "@return {!impl/Channel} <= [?err]"
  ([CP msg](cp-send CP msg nil))
  ([CP msg handle](cp-send CP msg nil nil))
  ([CP msg handle opts]
   (let [out (promise-chan)
         cb (fn [err](put! out [err]))
         args (remove nil? [(clj->js msg) handle opts cb])]
     (.apply (.-send (.-proc CP)) (.-proc CP) (into-array args))
     out)))


; this API is subject to change
(deftype ChildProcess [proc out]
  IChildProcess
  ILookup
  (-lookup [this k] (get (.props this) k))
  impl/ReadPort
  (take! [_ handler] (impl/take! out handler))
  Object
  (setEncoding [this enc]
    (.setEncoding (.-stdout proc) enc)
    (.setEncoding (.-stderr proc) enc)
    this)
  (kill [_] (.kill proc)) ;maybe unsafe? ; flag killed?
  (kill [_ sig] (.kill proc sig))
  (disconnect [_] (.disconnect proc))
  (write [_ chunk enc cb] (.write (.-stdin proc) chunk enc cb))
  (send [this msg] (cp-send this msg))
  (send [this msg handle] (cp-send this msg handle))
  (send [this msg handle opts] (cp-send this msg handle opts))
  (props [this]
    {:connected (.-connected proc)
     :stdout (.-stdout proc)
     :stdin (.-stdin proc)
     :stderr (.-stderr proc)
     :stdio (.-stdio proc)
     :pid (.-pid proc)}))

(defn child
  [proc]
  (let [out (cp->ch proc)]
    (->ChildProcess proc out)))