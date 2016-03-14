
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
    - separate out coercions in impls
    - stream-type specific impl attachers
      - setup print representations
      - specific methods
        * java.io.FileInputStream API etc
            - available
            - close
            - finalize
            - getChannel      
  + __types__
    - compile checks, jsDoc (simple opts cant be used with figwheel)
    - runtime checks, schema?
    - verify degenerate cases, type returns
    - verify docs
  + __copy__
    - [x] easiest: coerce args to streams and the just pipe it over?
    - [ ] Test
    - [ ] Options supported
      - __:buffer-size__  buffer size to use, default is 1024.
      - __:encoding__     encoding to use if converting between byte and char streams.
  + extend missing types with coercions & IOFactory
    - Object (throw in all cases)
    - Socket
    - byte-array
    - char-array        
  + verify opts keys through all paths. :append? :async? :stream?
    - should be :append like clojure semantics? "?" hints bool though
  + __line-seq  + test __
  + __xml-seq  + test __    
  + aFile? async reified file objects
    - constructor opt?
    - currently you have sync methods but option for async read/write
    - .aread vs .read methods to distinguish. rather than build based on opts?    
  + super-spit?
  + try/catch => bool macro, sync vs async
    - try-false, try-bool
  + test advanced compilation
  + script examples, w/ CLI args
  + https://github.com/Raynes/fs and other convenience stuff out there
  + java URL has unique set of methods that should be extended to goog.Uri
    - openStream -> opens connection to this URL and returns an input stream for reading
      - see IOFactory
      - https://docs.oracle.com/javase/7/docs/api/java/net/URL.html#openStream()

  + __zlib__
    - zip/unzip files? directories?
  + __transit w/ object stream??__
  + fs.watch, fs.watchFile, symlinks, realpath, chmod, chown readlink, fsync




### Issues
  + switch (.exists File) to non-deprecated impl
    - Use fs.statSync() or fs.accessSync() instead.
  + __encodings__
    - "hex"
    - "binary"
    - "base64"
    - test throughout, streams + file readers + writers
    - both direct constructors and indirection via option map passing all over the place
  * __readline__
    - line-seq needs readline stream, probably must be async (breaking from clj)
    - file reader needs a read, readline methods??
  * __Improve Error Handling__
    - defrecord SomeError [cause context ....]
  * misc
    - test ns specific temp file that doesnt have delete-on-exit listeners
    - if file is deleted make sure event listeners are removed? (delete on exit)
      -find way to manage deletion listeners
    + File streams need bettters docs w/ default option info
    + reified files lacking chmod methods etc
    * sslurp assumes file with extension, whereas slurp (should) opens reader variable types
    * delete-file should handle absolute paths, not just file objects
    * add mode to supported opts for reader & writer
      - streams, file api
      - (js/parseInt "0666" 8) ||   (js/Number "0o666")  , ES6 octal literal
    * change file & filestream type to resemble cljs type (currently is keyword)   


### Notes
  * biased towards sync calls, async makes for poor repl experience
  * consolidated URL & URI, goog.net.Uri is very java-ish
  * node streams close themselves, cleanup automatically?
  * polymorphic java constructors (ie java.io.File)
    necessitate alot of indirection
  * default-impl-obj + specify! is a cool pattern
