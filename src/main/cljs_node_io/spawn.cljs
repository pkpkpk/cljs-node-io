(ns cljs-node-io.spawn
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as casync :refer [put! take! chan <! promise-chan close!]]
            [cljs.core.async.impl.protocols :as impl]
            [cljs-node-io.proc :as proc]
            [cljs-node-io.async :as nasync]))

(defn cp->ch
  "Wraps all ChildProcess events into messages put! on a core.async channel.
   The chan will close when all underlying data sources close.
   See doc for cljs-node-io.proc/child for opts.
   If key is provided, msgs below are prefixed as [key msg]
     [:error [js/Error]]
     [:disconnect nil]
     [:message [#js{} ?handle]]
     [:exit [?code ?signal]]
     [:close [?code ?signal]]
     [:stdout [:data [chunk]]]
     [:stdout [:error [js/Error]]]
     [:stdout [:end nil]]
     [:stderr [:data [chunk]]]
     [:stderr [:error [js/Error]]]
     [:stderr [:end nil]]
     [:stdin [:error [js/Error]]]
     [:stdin [:end nil]]
   @return {!cljs.core.async.impl.protocols/Channel}"
  ([proc](cp->ch proc nil))
  ([proc {:keys [key buf-or-n] :or {buf-or-n 10}}]
   (let [tag-xf (fn [tag] (if-not key (map #(conj [tag] %)) (map #(assoc-in [key [tag]] [1 1] %))))
         stdout-ch (nasync/readable-onto-ch (.-stdout proc) (chan buf-or-n (tag-xf :stdout)) ["close"])
         stderr-ch (nasync/readable-onto-ch (.-stderr proc) (chan buf-or-n (tag-xf :stderr)) ["close"])
         stdin-ch  (nasync/writable-onto-ch (.-stdin proc)  (chan buf-or-n (tag-xf :stdin))  ["close"])
         proc-ch (chan buf-or-n)
         exits (atom #{"close" "exit"})
         exit-cb (fn [ev]
                   (swap! exits disj ev)
                   (if (empty? @exits)
                     (close! proc-ch)))
         out (casync/merge [stdin-ch stdout-ch stderr-ch proc-ch])]
     (doto proc
       ; missing signal events, SIGINT etc
       (.on "error" (fn [e]
                      (let [d [:error [e]]]
                        (put! proc-ch (if-not key d (conj [key] d))))))
       (.on "exit" (fn [code signal]
                     (let [d [:exit [code signal]]]
                       (put! proc-ch (if-not key d (conj [key] d)) #(exit-cb "exit")))))
       (.on "close" (fn [code signal]
                      (let [d [:close [code signal]]]
                        (put! proc-ch (if-not key d (conj [key] d)) #(exit-cb "close"))))))
     (when (.-send proc)
       (doto proc
         (.on "message" (fn [msg sendHandle]
                          (let [d [:message [msg sendHandle]]]
                            (put! proc-ch (if-not key d (conj [key] d) )))))
         (.on "disconnect" (fn []
                             (let [d [:disconnect nil]]
                               (put! proc-ch (if-not key d (conj [key] d))))))))
     out)))

(defn- cp-send
  "@return {!impl/Channel} <= [?err]"
  ([CP msg](cp-send CP msg nil))
  ([CP msg handle](cp-send CP msg nil nil))
  ([CP msg handle opts]
   (assert (.-send (.-proc CP)) "ChildProcess.send is only applicable to forks")
   (let [out (promise-chan)
         cb (fn [err](put! out [err]))
         args (remove nil? [(clj->js msg) handle opts cb])]
     (.apply (.-send (.-proc CP)) (.-proc CP) (into-array args))
     out)))

(defn- ^boolean cp-write
  "Defers to stdin.write, but skips writing when the stream has closed, returning
   false (instead of emitting error to an already closed channel)

   Calls the supplied callback once the data has been fully handled.
   If an error occurs, the callback may or may not be called with the error as its first argument.
   Detect write errors via CP :error events.

   The return value indicates whether the written chunk was buffered internally and
   the buffer has exceeded the highWaterMark configured when the stream was created.
   If false is returned, further attempts to write data to the stream should be paused
   until the 'drain' event is emitted.
   @return {!boolean}"
  ([cp chunk](cp-write chunk nil nil))
  ([cp chunk enc](cp-write chunk enc nil))
  ([cp chunk enc cb]
   (if ^boolean (.-writable (.-stdin (.-proc cp)))
     (.write (.-stdin (.-proc cp)) chunk enc cb)
     false)))

(deftype ChildProcess [proc out]
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
  (write [this chunk](cp-write this chunk))
  (write [this chunk enc](cp-write this chunk enc))
  (write [this chunk enc cb](cp-write this chunk enc cb))
  ;fork only
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

(defn spawn
  "@param {!string} cmd :: command to execute in a shell
   @param {!IVector} args :: args to the shell command
   @param {!IMap} opts :: execution options + cp-out-options
    + execution opts:
      -
    + cp-out options:
      :buf-or-n -> passed to all subchannels, defaults to 10
      :key  -> is used to prefix all emitted values. Use to identify and route data.
        ex no key: [:stdout [:data ['some data']]]
        ex w/ key: ['my-child-proc' [:stdout [:data ['some data']]]]
   @return {!proc/PortedChildProcess} ChildProcess with a ReadPort"
  [cmd args {:keys [encoding] :as opts}]
  (assert (map? opts))
  (assert (string? cmd))
  (assert (and (seq args) (every? string? args)))
  (let [child-process (proc/spawn cmd args opts)
        out (cp->ch child-process opts)]
    (cond-> (->ChildProcess child-process out)
      encoding (.setEncoding encoding))))

(defn fork
  "Launch a node child process with an IPC Socket.
    + send values via .send()

   @param {!string} modulePath :: path to js file to run
   @param {!IVector} args :: arguments to the js file
   @param {!IMap} opts :: execution options + cp-out-options
    + execution opts:
      - :silent -> true
      - :stdio -> 'pipe'  disallow change
    + cp-out options:
      :buf-or-n -> passed to all subchannels, defaults to 10
      :key  -> is used to prefix all emitted values. Use to identify and route data.
        ex no key: [:stdout [:data ['some data']]]
        ex w/ key: ['my-child-proc' [:stdout [:data ['some data']]]]
   @return {!proc/PortedChildProcess} ChildProcess with a ReadPort"
  [modulePath args {:keys [encoding] :as opts}]
  (assert (string? modulePath))
  (when opts (assert (map? opts)))
  (when args (assert (and (seq args) (every? string? args))))
  (let [child-process (proc/fork modulePath args opts)
        encoding (or encoding "utf8")
        out (cp->ch child-process opts)]
    (cond-> (->ChildProcess child-process out)
      encoding (.setEncoding encoding))))