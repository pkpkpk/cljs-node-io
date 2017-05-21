# `[cljs-node-io "0.5.0"]`

[![Clojars Project](https://img.shields.io/clojars/v/cljs-node-io.svg)](https://clojars.org/cljs-node-io)

This is a port of clojure.java.io to clojurescript, in a way that makes sense for nodejs. The goal is to make the clojure programmer feel right at home, so most everything has the same signatures and semantics as their jvm counterparts. However many things necessarily work differently internally, and with some consequence. You can [read about the differences here](#differences-from-clojure)

#### Also included:
  + reified files with same api as java
  + slurp + spit
  + wrappers over node streams, child processes
  + convenience functions to make your scripting and repl'ing experience more pleasant
  + ~~compiled with [andare](https://github.com/mfikes/andare) so that~~ all the core async is bootstrap friendly

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

In the nodejs fs module, functions are asynchronous by default, and their synchronous versions have names with a `Sync` suffix. In *cljs-node-io*, functions are synchronous by default, and async versions have an `a` prefix.  For example, `cljs-node-io.core/slurp` is synchronous (just as jvm), whereas `cljs-node-io.core/aslurp` runs asynchronously. This convention simply saves you some thought cycles at the repl. Note that most of the time (scripting...) synchronous functions are fine and getting order guarantees from async code is not worth the hassle

#### IO Operations & Error Handling
  - all functions should throw at the call-site if given the wrong type.
  - Sync IO ops *will* throw on IO exceptions. Error handling is up to the user
  - in asynchronous functions, IO exceptions will be part of the channel yield. The convention here is to mimic *nodeback* style callbacks with channels yielding `[?err]` or `[?err ?data]` depending on the operation
```clojure
(go
 (let [[err data] (<! afn)]
   (if-not err
     (handle-result data)
     (handle-error err))))
```

 - for successful ops, errors will be nil. This lets you destructure the result and *branch on err*
 - note this is not transactional... some side effects may have occured despite an error

##### Predicates
  - Sync predicates do not throw on op errors, they catch the error and return false
  - Async predicates return chans that receive false on err. These channels only receive booleans.

<hr>

#### Getting Order Guarantees From Async Code
read more [here](https://nodejs.org/en/docs/guides/blocking-vs-non-blocking/)


```clojure
(require '[cljs-node-io.fs :as fs])

(fs/touch "/tmp/hello")

;; BAD! maybe astat is run before arename
(def rc (fs/arename "/tmp/hello" "/tmp/world"))
(def sc (fs/astat "/tmp/world"))

(go
  (let [[err] (<! rc)]
    (if-not err
      (let [[err st] (<! sc)]
        (if-not err
          (println (js/JSON.stringify st))
          (throw err)))
      (throw err))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; GOOD!  chain the calls together
(go
  (let [[err] (<! (fs/arename "/tmp/hello" "/tmp/world"))]
    (if-not err
      (let [[err st] (<! (fs/astat "/tmp/world"))]
        (if-not err
          (println (js/JSON.stringify st))
          (throw err)))
      (throw err))))
```

<hr>

### Differences from Clojure
  + Node runs an asynchronous event loop & all IO is driven under the hood by [libuv](http://libuv.org/). Construction of streams involves creating a object within the js-vm and returning it to the user synchronously so that listeners may be attached. Calls to listeners are then scheduled using [js/process.nextTick](https://nodejs.org/dist/latest-v7.x/docs/api/process.html#process_process_nexttick_callback_args). This means you fundamentally cannot create a stream and consume it synchronously... you must instead create the stream and attach handlers to its emitted events.
    - clojure.java.io coerces everything into streams synchronously, and reads and writes from there. This strategy cannot work in node

  + To preserve synchronous semantics, `slurp` for example uses memory consuming fs.readFileSync. This is fine for small files and repl sessions. If you need to read larger files, restructure your program to accommodate node streams. Luckily node streams mostly manage themselves.


  + no reader + writer types, not really necessary
  + no URL type, just goog.net.Uri
  + javascript does not have a character type
  + no java-style char/byte arrays, just nodejs buffers

