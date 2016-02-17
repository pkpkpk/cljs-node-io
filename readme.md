
# cljs-node-io


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

# todo
  + aFile? add file-global toggle for async or separate named methods?
    - currently you have sync methods but option for async read/write
    - .aread vs .read methods to distinguish. rather than build based on opts?
  + try/catch=>bool macro, sync vs async
  * jsDoc
  * verify degenerate cases, type returns
  * verify opts keys through all paths. :append? :async? :stream?
    - should be :append like clojure semantics? "?" hints bool though
  * https://github.com/Raynes/fs
  * java URL has unique set of methods that should be extended to goog.Uri
    * openStream -> opens connection to this URL and returns an input stream for reading
      - see IOFactory
      - https://docs.oracle.com/javase/7/docs/api/java/net/URL.html#openStream()
  * __line-seq  + test __
  * __xml-seq  + test __
  * __Streams__
    - reader + writer
    - types
        - file
        - object streams,
        - duplex streams,
        - transform streams,
        - gcl node-object-stream ??
    - java.io.FileInputStream API
      - available
      - close
      - finalize
      - getChannel
      - getFD
  *  __zlib__
    - zip/unzip files?

  * __transit object stream??__

  * __PROBLEMS__
    - test ns specific temp file that doesnt have delete-on-exit listeners
    - if file is deleted make sure event listeners are removed? (delete on exit)
      -find way to manage deletion listeners
    * slurp + spit encodings are broken
    * sslurp assumes file with extension, whereas slurp (should) opens reader variable types
    * delete-file should handle absolute paths, not just file objects
    * add mode to supported opts for reader & writer
      - streams, file api
      - (js/parseInt "0666" 8)   ===   0o666 ES6 octal literal
    * change file & filestream type to resemble cljs type (currently is keyword)    


  * __readline__
    - line-seq needs readline stream, probably must be async (breaking from clj)
    - file reader needs a read, readline methods.


  * __Improve Error Handling__
   - defrecord SomeError [cause context ....]



## Notes
  * biased towards sync calls, async makes for poor repl experience
  * consolidated URL & URI, goog.net.Uri is great
  * node streams close themselves, cleanup automatically?
  * polymorphic java constructors (ie java.io.File)
    necessitate alot of indirection
  * default-impl-obj + specify! is a cool pattern
