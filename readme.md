# `[cljs-node-io "0.2.0"]`

[![Clojars Project](https://img.shields.io/clojars/v/cljs-node-io.svg)](https://clojars.org/cljs-node-io)

This is a port of clojure.java.io to clojurescript, in a way that makes sense for nodejs. The goal is to make the clojure programmer feel right at home, so most everything has the same signatures and semantics as their jvm counterparts. However many things necessarily work differently internally, and with some consequence. You can [read about the differences here](#differences-from-clojure)

#### Also included:
  + reified files with same api as java
  + slurp + spit
  + wrappers over node streams
  + convenience functions to make your scripting and repl'ing experience more pleasant
  + ~~compiled with [andare](https://github.com/mfikes/andare) so that the all the core async is bootstrap friendly~~

<hr>

#### In your repl session & scripts

```clojure
(require '[cljs-node-io.core :as io :refer [slurp spit]])

(def data [{:foo 42} {:foo 43}])

(spit "data.edn"  data)

(= data (read-string (slurp "data.edn"))) ;=> true

```

#### In your app

```clojure
;; write asynchronously using core.async
(go
  (let [[err] (<! (io/aspit "data.edn" data))]
    (if-not err
      (println "you've successfully written to 'data.edn'")
      (println "there was an error writing: " err))))

;; read asynchronously using core.async
(go
  (let [[err datastring] (<! (io/aslurp "data.edn"))]
    (if-not err
      (handle-data (read-string datastring))
      (handle-error err))))

```
<hr>


### Differences from Clojure
  + Node runs an asynchronous event loop & is event driven. This means you can't do things like create a stream and consume it synchronously (the underlying stream may not be ready yet)... you must instead create the stream and attach handlers to its emitted events.
    - clojure.java.io coerces everything into streams and reads and writes from there. This strategy cannot work in node


  + In the nodejs fs module, functions are asynchronous by default, and their synchronous versions have names with a `Sync` suffix. In *cljs-node-io*, functions are synchronous by default, and async versions have an `a` prefix.  For example, `cljs-node-io.core/slurp` is synchronous (just as jvm), whereas `cljs-node-io.core/aslurp` runs asynchronously. This convention simply saves you some thought cycles at the repl. Note that most of the time synchronous functions are fine and getting order guarantees from async code is not worth the hassle


  + To preserve synchronous semantics, `slurp` for example uses memory consuming fs.readFileSync. This is fine for small files and repl sessions. If you need to read larger files, restructure your program to accommodate node streams. Luckily node streams mostly manage themselves and are awesome.


  + no reader + writer types, not really necessary
  + no URL type, just goog.net.Uri
  + javascript does not have a character type
  + no java-style char/byte arrays, just nodejs buffers

