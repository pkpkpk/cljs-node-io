(ns cljs-node-io.async
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as casync :refer [put! take! close! chan alts! promise-chan]]))

(defn go-proc
  "sets up an infinite go-loop with a kill method. Continuously takes from in
   and applies supplied handler to value asynchronously. If killed or input closes,
   exits the loop, and calls the supplied exit cb. If (exit? (<! in)) the go-proc
   is killed, though that same val is handled and events may still be processed 
   depending on next round of alts!. The error handler shoul be a function of 1 arg
   and is called errors from handler/exit? applications to incoming values. The go-proc
   will continue processing after errors & must be manually killed."
  ([in handler error-handler](go-proc in handler error-handler nil nil))
  ([in handler error-handler exit-cb](go-proc in handler error-handler exit-cb nil))
  ([in handler error-handler exit-cb exit?]
   {:pre [(implements? cljs.core.async.impl.protocols/Channel in)
          (fn? handler)
          (fn? error-handler)
          (if exit-cb (fn? exit-cb) true)
          (if exit? (fn? exit?) true)]}
   (let [kill-ch (promise-chan)
         gblock (go-loop []
                  (let [[v c] (alts! [kill-ch in])]
                    (if v
                      (do 
                        (if (and exit?
                                 (try
                                   (exit? v)
                                   (catch js/Error e
                                     (let [e' {:error e
                                               :msg "go-proc: uncaught exit condition error"}]
                                       (error-handler e'))
                                     false)))
                          (close! kill-ch))
                        (try
                          (handler v)
                          (catch js/Error e
                            (let [e' {:error e
                                      :msg "go-proc: uncaught handler error"}]
                              (error-handler e'))))
                        (recur))
                      (do 
                        (if exit-cb (exit-cb))
                        (close! kill-ch)))))]
      (set! (.-active gblock) true)
      (take! kill-ch #(set! (.-active gblock) false))
      (specify! gblock Object
        (kill [_](close! kill-ch))))))


(def stream (js/require "stream"))

(defn- handle-vals [vals] (if-not (empty? vals) (vec vals)))

(defn event-onto-ch
  "Converts a event cb into a core.async put!. Event Cb params are put into vectors.
   cb()    -> [:event nil]
   cb(a)   -> [:event [a]]
   cb(a,b) -> [:event [a b]]
   @param {!events.EventEmitter} emitter
   @param {!Channel} out :: unblocking buffers will drop data, including errors
   @param {!string} event
   @return {!events.EventEmitter}"
  [emitter out event]
  (assert (string? event))
  (if (or (casync/unblocking-buffer? out) (not (instance? stream.Readable emitter)))
    (.on emitter event (fn [& vals] (put! out [(keyword event) (handle-vals vals)])))
    (.on emitter event
     (fn [& vals]
       (this-as this
        (let [val [(keyword event) (handle-vals vals)]]
          (when-not (casync/offer! out val)
            (.pause this)
            (put! out val #(.resume this)))))))))

(defn readable-onto-ch
  "Generic interface so you can write reloadable handlers. Removes the need to
   manage stream listener reload lifecycle. Supply your configured chan. 'data'
   'end',readstream level 'error' events are caught and put! as [:event cb-arg].
   Be aware that nonblocking buffers may drop error events too. Parking chans
   behave just like stream.pipe methods. Note pending put! limits"
  ([rstream out-ch] (readable-onto-ch rstream out-ch nil))
  ([rstream out-ch events] ;ie ["close"]
   (let [common ["data" "error" "end"]
         events (if-not events common (concat common events))]
     (doseq [ev events](event-onto-ch rstream out-ch ev))
     out-ch)))

(defn cp->ch
  "Wraps all ChildProcess events into messages put! on a core.async channel :
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
   [:stdin [:end nil]]"
  ([proc](cp->ch proc 10))
  ([proc buf-or-n]
    (let [stdout (readable-onto-ch (.-stdout proc) (chan buf-or-n (map #(conj [:stdout] %))))
          stderr (readable-onto-ch (.-stderr proc) (chan buf-or-n (map #(conj [:stderr] %))))
          out (casync/merge [stdout stderr])]
      (doto (.-stdin proc)
        (.on "error" (fn [e] (put! out [:stdin [:error [e]]])))
        (.on "end" (fn [] (put! out [:stdin [:end nil]]))))
      (doto proc
        ; missing signal events, SIGINT etc
        (.on "error" (fn [e] (put! out [:error [e]])))
        (.on "exit" (fn [code signal](put! out [:exit [code signal]])))
        (.on "close" (fn [code signal](put! out [:close [code signal]]))))
      (when (.-send proc)
        (doto proc
          (.on "message" (fn [msg sendHandle] (put! out [:message [msg sendHandle]])))
          (.on "disconnect" (fn [](put! out [:disconnect nil])))))
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