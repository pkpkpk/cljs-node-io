
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




### Notes
  + biased towards sync calls, async makes for poor repl experience
  + no URL type, just goog.net.Uri (which is great & very java-ish)
  + clojure on JVM exploits inheritance for typing, not available here
  + fudging types with keywords, simplest
  + no java-style char/byte arrays, just nodejs buffers
  + node streams manage themselves, are awesome. no readers necessary

### Differences from Clojure