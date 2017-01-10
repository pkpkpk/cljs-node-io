(ns cljs-node-io.async
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as casync :refer [put! take! close! chan alts! promise-chan]]))

(defn go-proc
  "Sets up an infinite go-loop with a kill method. Continuously takes from _in_
   and applies supplied handler to the read value asynchronously. If killed or input closes,
   exits the loop, and calls the supplied exit cb. If (exit? (<! in)) the go-proc
   is killed, though that same val is handled + events may still be processed
   depending on next round of alts!. The error handler should be a function of 1 arg
   and is called with errors from handler+exit? applications to incoming values.
   The go-proc will continue processing after errors & must be manually killed."
  ([in handler](go-proc in handler nil nil nil))
  ([in handler error-handler](go-proc in handler error-handler nil nil))
  ([in handler error-handler exit-cb](go-proc in handler error-handler exit-cb nil))
  ([in handler error-handler exit-cb exit?]
   {:pre [(implements? cljs.core.async.impl.protocols/ReadPort in)
          (fn? handler)
          (if error-handler (fn? error-handler) true)
          (if exit-cb (fn? exit-cb) true)
          (if exit? (fn? exit?) true)]}
   (let [eh (or error-handler (js/console.error.bind js/console))
         kill-ch (promise-chan)
         gblock (go-loop []
                  (let [[v c] (alts! [kill-ch in])]
                    (if v
                      (do 
                        (if (and exit?
                                 (try
                                   (exit? v)
                                   (catch js/Error e
                                     (eh  {:error e :msg "go-proc: uncaught exit condition error"})
                                     false)))
                          (close! kill-ch))
                        (try
                          (handler v)
                          (catch js/Error e
                            (eh {:error e :msg "go-proc: uncaught handler error"})))
                        (recur))
                      (do 
                        (if exit-cb (exit-cb))
                        (close! kill-ch)))))]
      (set! (.-active gblock) true)
      (take! kill-ch #(set! (.-active gblock) false))
      (specify! gblock
        IPrintWithWriter
        (-pr-writer [this writer opts]
          (-write writer "#object [cljs-node-io.async/go-proc]"))
        Object
        (kill [_](close! kill-ch))))))


(defprotocol IMux
  (admux [this port] "add a Readport to the Mux")
  (unmux [this port] "remove a Readport from the Mux")
  (unmux-all [this] "remove all ports from Mux reading"))

(defn mux
  "Inspired by core.async/mix, mux is a 'multiplex' of readports. Instead of building
   a mix and then having a separate consumption and processing step, this skips
   the middle-man and continuously processes values from a collection of ReadPorts
   using the given handler. This saves an extra chan+go-loop and skips unecessary
   asynchronicity. It can be view as an *extensible* border between asynchronous
   data reception and synchronous handling of that data (though there is no reason
   you cannot reintroduce asynchronicity downstream).

   Added ReadPorts are continually read from using a shared alts! go-loop, and
   the user supplied handler is called with the port & recieved values. When a
   port closes, it is removed from the mux. An empty mux will persist until killed.
   Mux knows nothing about msg content or ReadPorts' lifecycle.

   Mux is meant to serve as an entry point for external connections into your
   application. Structure your input values and handler fn for data based routing
   @return {!IMux}"
  ([handler](mux handler nil))
  ([handler err-handler]
   (let [eh (or err-handler (js/console.error.bind js/console))
         ports (atom #{})
         change-ch (chan)
         kill-ch  (promise-chan)
         calc-state #(vec (conj @ports change-ch kill-ch))
         remove-port #(swap! ports disj %)
         gblock
         (go-loop [reads (calc-state)]
           (let [[v port] (alts! reads)]
             (cond
               (= port kill-ch) nil
               (= port change-ch) (recur (calc-state))
               (nil? v) (do (remove-port port) (recur (calc-state)))
               :else ;handle
               (do
                 (try (handler port v)
                   (catch js/Error e
                     (eh {:e e :v v :msg "mux: data handler error"})))
                 (recur reads)))))]
     (set! (.-active gblock) true)
     (take! kill-ch #(set! (.-active gblock) false))
     (specify! gblock
       IPrintWithWriter
       (-pr-writer [this writer opts]
         (-write writer "#object [cljs-node-io.async/mux]"))
       IMux
       (admux [_ port]
          (if-not ^boolean (casync/poll! kill-ch)
            (do (swap! ports conj port) (put! change-ch true) port)
            false))
       (unmux [_ port] (do (swap! ports disj port) (put! change-ch true) port))
       (unmux-all [_] (do (reset! ports #{}) (put! change-ch true)))
       Object
       (ports [_] @ports)
       (kill [_](do (reset! ports nil) (casync/put! kill-ch true)))))))


(def stream (js/require "stream"))

(defn- handle-vals [event vals] (if-not (empty? vals) (vec vals)))

(defn event-onto-ch
  "Converts a event cb into a core.async put!. Event Cb params are put into vectors.
   cb()    -> [:event nil]
   cb(a)   -> [:event [a]]
   cb(a,b) -> [:event [a b]]
   @param {!events.EventEmitter} emitter
   @param {!Channel} out :: unblocking buffers will drop data, including errors
   @param {!string} event
   @return {!events.EventEmitter}"
  ([emitter out event](event-onto-ch emitter out event nil))
  ([emitter out event cb]
   (assert (string? event))
   (if-not (instance? stream.Readable emitter)
     (.on emitter event
        (fn [& vals]
          (put! out [(keyword event) (handle-vals event vals)]  #(if cb (cb)))))
     (.on emitter event
        (fn [& vals]
          (this-as this
            (let [val [(keyword event) (handle-vals event vals)]]
              (if ^boolean (casync/offer! out val)
                (if cb (cb event))
                (do
                  (.pause this)
                  (put! out val #(do (.resume this) (if cb (cb event)))))))))))))

(defn readable-onto-ch
  "Generic interface so you can write reloadable handlers. Removes the need to
   manage stream listener reload lifecycle. Supply your configured chan. 
   'data', 'end', & 'error' events are caught and put! as [:event [cb-arg0 cb-arg1]].

   If your stream fires other events (such as 'close'), provide them as a vector
   of strings. 'close' is automatically recognized as an exit condition. The chan
   will be closed when exit conditions have been met.

   Be aware that nonblocking buffers may drop error events too. Parking chans
   behave just like stream.pipe methods. Note pending put! limits
   @return {!cljs.core.async.impl.protocols/Channel}"
  ([rstream out-ch] (readable-onto-ch rstream out-ch nil))
  ([rstream out-ch events] ;ie ["close"]
   (if events (assert (and (coll? events) (every? string? events))))
   (let [close? (some #(= "close" %) events)
         exits (atom (into #{} (if close? ["close" "end"] ["end"])))
         exit-cb (fn [ev]
                   (swap! exits disj ev)
                   (if (empty? @exits) (close! out-ch)))
         common ["data" "error" "end"]
         events (into #{} (if-not events common (concat common events)))]
     (doseq [ev events]
       (if (@exits ev)
         (event-onto-ch rstream out-ch ev exit-cb)
         (event-onto-ch rstream out-ch ev)))
     out-ch)))

(defn writable-onto-ch
  "Generic interface so you can write reloadable handlers. Removes the need to
   manage stream listener reload lifecycle. Supply your configured chan.
   'finish', 'drain', & 'error' events are caught and put! as [:event [cb-arg0 cb-arg1]].

   If your stream fires other events (such as 'close'), provide them as a vector
   of strings. 'close' is automatically recognized as an exit condition. The chan
   will be closed when exit conditions have been met.

   Be aware that nonblocking buffers may drop error events too. Parking chans
   behave just like stream.pipe methods. Note pending put! limits
   @return {!cljs.core.async.impl.protocols/Channel}"
  ([wstream out-ch] (writable-onto-ch wstream out-ch nil))
  ([wstream out-ch events] ;ie ["close"]
   (if events (assert (and (coll? events) (every? string? events))))
   (let [close? (some #(= "close" %) events)
         exits (atom (into #{} (if close? ["close" "finish"] ["finish"])))
         exit-cb (fn [ev]
                   (swap! exits disj ev)
                   (if (empty? @exits) (close! out-ch)))
         common ["finish" "error" "drain"]
         events (into #{} (if-not events common (concat common events)))]
     (doseq [ev events]
       (if (@exits ev)
         (event-onto-ch wstream out-ch ev exit-cb)
         (event-onto-ch wstream out-ch ev)))
     out-ch)))


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
         stdout-ch (readable-onto-ch (.-stdout proc) (chan buf-or-n (tag-xf :stdout)) ["close"])
         stderr-ch (readable-onto-ch (.-stderr proc) (chan buf-or-n (tag-xf :stderr)) ["close"])
         stdin-ch  (writable-onto-ch (.-stdin proc)  (chan buf-or-n (tag-xf :stdin))  ["close"])
         proc-ch (chan buf-or-n)
         exits (atom #{"close" "exit"})
         exit-cb (fn [ev] (swap! exits disj ev) (if (empty? @exits) (close! proc-ch)))
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

(defn sock->ch
  "[:data [chunk]]
   [:error [js/Error]]
   [:connect nil]
   [:close [?js/Error]]
   [:drain nil]
   [:lookup [?js/Error address ?family host]]
   [:timeout nil]
   [:end nil]"
  ([socket](sock->ch socket 10))
  ([socket buf-or-n]
   (let [out (chan buf-or-n)
         sock-events ["lookup" "drain" "timeout" "close" "connect"]
         _ (readable-onto-ch socket out sock-events)]
     out)))

(defn server->ch
  ([server] (server->ch server 10))
  ([server buf-or-n]
    (let [out (chan buf-or-n)]
      (doto server
        (.on "close" (fn [] (put! out [:close nil])))
        (.on "error" (fn [e] (put! out [:error [e]])))
        (.on "listening" (fn [e] (put! out [:listening nil])))
        (.on "connection" (fn [socket] (put! out [:connection [socket]]))))
      out)))