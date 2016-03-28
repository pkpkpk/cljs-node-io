
# cljs-node-io

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

# todo
  + __streams__
    - tests & examples
    - cleanup
    - consolidate docs
    - methods like pipe take option maps, in docs are cljs maps, shouldnt be
    - buffer stream methods
    - consider writev method for buffer BufferWriteStream
  + extend Buffer with useful protocols
    - [x] IEquiv
    - (into [] (array-seq <Buffer>))
  + __copy__
    - [x] easiest: coerce args to streams and the just pipe it over?
    - directories?
    - [ ] Test
    - [ ] Options supported
      - __:buffer-size__  buffer size to use, default is 1024.
      - __:encoding__     encoding to use if converting between byte and char streams.


  + __types__
    - compile checks, jsDoc (simple opts cant be used with figwheel)
    - runtime checks, schema?
    - verify degenerate cases, type returns
    - verify docs
  + extend missing types with coercions & IOFactory
    - ~~Object~~
    - Socket
  + doc strings
  + test IOFactory on all supported types
  + verify opts keys through all paths. :append? :async? :stream?
    - should be :append like clojure semantics? "?" hints bool though
  + line-seq  + test
  + __readline__
    - line-seq needs readline stream, probably must be async (breaking from clj)
    - file reader needs a read, readline methods??
  * __Improve Error Handling__
    - defrecord SomeError [cause context ....]  
  + xml-seq  + test  
  + aFile? async reified file objects
    - constructor opt?
    - currently you have sync methods but option for async read/write
    - .aread vs .read methods to distinguish. rather than build based on opts?    
  + super-spit?
  + try/catch => bool macro, sync vs async
    - try-false, try-bool
  + script examples, w/ CLI args
  + https://github.com/Raynes/fs and other convenience stuff out there
  + java URL has unique set of methods that could be extended to goog.Uri
    - openStream -> opens connection to this URL and returns an input stream for reading
      - see IOFactory
      - https://docs.oracle.com/javase/7/docs/api/java/net/URL.html#openStream()

  + __zlib__
    - zip/unzip files? directories?
  + __transit w/ object stream??__
  + fs.watch, fs.watchFile, symlinks, realpath, chmod, chown readlink, fsync

``` clj
(def default-streams-impl
  {:make-reader (fn [x opts] (make-reader (make-input-stream x opts) opts))
   :make-writer (fn [x opts] (make-writer (make-output-stream x opts) opts))
   :make-input-stream (fn [x opts]
                        (throw (IllegalArgumentException.
                                (str "Cannot open <" (pr-str x) "> as an InputStream."))))
   :make-output-stream (fn [x opts]
                         (throw (IllegalArgumentException.
                                 (str "Cannot open <" (pr-str x) "> as an OutputStream."))))})

(extend Socket
  IOFactory
  (assoc default-streams-impl
    :make-input-stream (fn [^Socket x opts] (make-input-stream (.getInputStream x) opts))
    :make-output-stream (fn [^Socket x opts] (make-output-stream (.getOutputStream x) opts))))

```

### Issues

  + switch (.exists File) to non-deprecated impl
    - Use fs.statSync() or fs.accessSync() instead.
  + misc
    - if file is deleted make sure event listeners are removed? (delete on exit)
      -find way to manage deletion listeners
    + File streams need better docs w/ default option info
    + reified files lacking chmod methods etc
    + sslurp assumes file with extension, whereas slurp (should) opens reader variable types
    + delete-file should handle absolute paths, not just file objects
    + change file & filestream type to resemble cljs type (currently is keyword)   


### Notes
  + biased towards sync calls, async makes for poor repl experience
  + no URL type, just goog.net.Uri (which is great & very java-ish)
  + clojure on JVM exploits inheritance for typing, not available here
  + fudging types with keywords, simplest
  + no java-style char/byte arrays, just nodejs buffers
  + node streams manage themselves, are awesome. no readers necessary
