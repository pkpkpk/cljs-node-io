
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
    - clojure.java.io coerces everything into streams and reads and writes from there. This strategy cannot work in node. 
    - To preserve synchronous semantics, slurp for example uses memory consuming fs.readFileSync. This is fine for small files and repl sessions. If you need to read larger files, restructure your program to accommodate node streams. Luckily core.async makes this easy!


  + node streams manage themselves, are awesome. no reader+writer interfaces necessary
  + no URL type, just goog.net.Uri (which is great & very java-ish)
  + javascript does not have a character type
  + no java-style char/byte arrays, just nodejs buffers
  + clojure on JVM exploits inheritance for typing, not available here
  
  