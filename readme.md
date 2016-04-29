
# cljs-node-io

This is a port of clojure.java.io to clojurescript, in a way that makes sense for nodejs. The goal is to make the clojure programmer feel right at home, so most everything has the same signatures and semantics as their jvm counterparts. However many things necessarily work differently internally, and with some consequence. You can [read about the differences here](#differences-from-clojure)

#### Also included:
  + reified files with same api as java
  + slurp + spit !
  + wrappers over node streams (which are awesome!)
  + convenience functions to make your scripting and repl'ing experience more pleasant

<hr>
## Use

#### In your repl session

```clj
(require '[cljs-node-io.core :as io :refer [spit sslurp]])

(def data [{:foo 42} {:foo 43}])

(spit "data.edn"  data)

(= data (sslurp "data.edn")) ;=> true

```

#### In your app

```clj
;; write asynchronously
(go
  (when-let [written (<! (io/aspit "data.edn" data))]
    (if (true? written)
      (println "you've successfully written to 'foo.edn'")
      (println "there was an error writing: " written))))

;; read asynchronously
(go
  (when-let [data (<! (io/saslurp "data.edn"))]
    (if (io/error? data)
      (handle-error data)
      (do-stuff data))))

```
<hr>

### Differences from Clojure
  + Node runs an asynchronous event loop & is event driven. This means you can't do things like create a stream and consume it synchronously (that is, on the same event-loop tick)... you must instead create the stream and attach handlers to its emitted events.
    - clojure.java.io coerces everything into streams and reads and writes from there. This strategy cannot work in node


  + To preserve synchronous semantics, slurp for example uses memory consuming fs.readFileSync. This is fine for small files and repl sessions. If you need to read larger files, restructure your program to accommodate node streams. Luckily core.async makes this easy!


  + In the nodejs, functions are asynchronous by default, and their synchronous versions have names with a `Sync` suffix. Here functions are synchronous by default, and async versions have an `a` prefix. This convention simply saves you some thought cycles at the repl. *You should use the async versions in your apps!*


  + node streams mostly manage themselves, are awesome.
  + no reader + writer types, not really necessary
  + no URL type, just goog.net.Uri
  + javascript does not have a character type
  + no java-style char/byte arrays, just nodejs buffers
  + clojure on JVM exploits inheritance for typing, not available here
  
  