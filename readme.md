
## clojure.java.io -> 'cljs-node-io.core
  - ##### https://clojure.github.io/clojure/clojure.java.io-api.html

  * [ ]__copy__
    - [ ] Test
    - ```(copy input output & opts)```
    - Copies input to output.  Returns nil or throws IOException.
    - __Input__ can be:
      - [ ] InputStream
      - [ ] Reader
      - [ ] File
      - [ ] byte[]
      - [ ] String
    - __Output__ may be:
      - [ ] OutputStream
      - [ ] Writer
      - [ ] File
      * [ ] string (coerced to file, not in clj api)
    - Options are key/value pairs and may be one of
      - __:buffer-size__  buffer size to use, default is 1024.
      - __:encoding__     encoding to use if converting between byte and char streams.      
    * __COMBINATIONS__:
      * [x] file file sync
      * [ ] file file stream
      * [x] file string sync
      * [ ] file string stream      
      * [x] string string sync
      * [ ] string string stream                        

  * [ ]__reader__
  * [ ]__writer__
  * ~~[ ]__resource__ (??)~~
  * __input-stream__
    * [ ] inputstream
    * [ ] File
    * [ ] URI, URL
    * [ ] Socket
    * [ ] byteArray
    * [ ] string
  * __output-stream__
    * [ ] outputstream
    * [ ] File
    * [ ] URI, URL
    * [ ] Socket
    * [ ] byteArray
    * [ ] string


## extras
  *  ### aspit, aslurp, asslurp?
  * __spit__
    - [x] sync
    - [x] async   
  * __slurp__
    - [x] sync
     -  NOT bufferedFileReader+FileStream as in clojure. Nodejs's streams are created
      asynchronously and would require slurp to return a channel. This uses
      FS.readFileSync, fine for small files. Use FileInputStream for more flexibility
    - [x] async
  * [x]__sslurp__
    - *super* slurp, convenience over slurp
    - automatically reads edn+json file into clj data-structures
  * [ ]__file-seq + test__  
  * [ ]__line-seq  + test __
  * [ ]__xml-seq  + test __

;java.io.FileInputStream API
; available
; close
; finalize
; getChannel
; getFD


  * [ ] __static File[]	listRoots()__
    - List the available filesystem roots.


<hr>
# todo
  + aFile? add file-global toggle for async or separate named methods?
    - currently you have sync methods but option for async read/write
  + try/catch=>bool macro, sync vs async
  - add mode to supported opts for reader & writer,  (js/parseInt "0666" 8) , === 0o666 ES6 octal literal
  * verify opts keys through all paths. :append? :async? :stream?
    - should be :append like clojure semantics
  * clj tests rely on correctness of java classes, so mock java
     stuff all needs appropriate tests and full api
  * verify degenerate cases, type returns
  * refactor streams, ditch specify! pattern, just manage path better?
  * path type for normalizing across platforms?
  * error handling can be better, move away from generic js/Error.
  * gcl node-object-stream support
  * object streams, duplex streams, transform streams
  * support other runtimes? JSC, nashorn
  * sugar for native streams, duplex + transform too
  * https://github.com/Raynes/fs
  * ###### java URL has unique set of methods that should be extended to goog.Uri
    * openStream -> opens connection to this URL and returns an input stream for reading
      https://docs.oracle.com/javase/7/docs/api/java/net/URL.html#openStream()
  * test isFd?
  * slurp + spit encodings are broken
  * delete-file should handle absolute paths, not just file objects
  * file reader needs a read, readline methods.
  * line-seq needs stream, probably must be async (breaking from clj)
  * defrecord SomeError [cause context ....]



## examples to do
* streaming mp3 through websocket
* clojure.data.csv port, m3u example?
* cookbook io examples
* large csv file reading via streamsk
* transit
* encodings
* webcrawler?




### Notes
  * biased towards sync calls, async makes for poor repl experience
  * consolidated URL & URI, goog.net.Uri is great
  * node streams close themselves, cleanup automatically?
  * polymorphic java constructors (ie java.io.File)
    necessitate alot of indirection
  * default-impl-obj + specify! is a cool pattern

; read [] -> int , reads a byte of data from this inputstream
; read [^byteArray b] -> int ,  Reads up to b.length bytes of data from this input stream into an array of bytes.
; read [^byteArray b, ^int off, ^int len] -> int ,   Reads up to len bytes of data from this input stream into an array of bytes.
