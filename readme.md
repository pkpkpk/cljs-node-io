# clojure.java.io

  * __~~as-relative-path~~__
  * __copy__
  * __~~delete-file~~__
  * __~~file~~ __
  * __make-parents__
  * __reader__
  * __writer__
  * __resource__ (??)
  * __~~spit~~__
  * __~~slurp~~__
  * __~~file-seq~~__  
  * __line-seq__  
  * __input-stream__
    * inputstream
    * File
    * URI, URL
    * Socket
    * byteArray
    * string
  * __output-stream__
    * inputstream
    * File
    * URI, URL
    * Socket
    * byteArray
    * string
  * #### tests

# todo
  * clj tests rely on correctness of java classes, so mock java
     stuff all needs appropriate tests and full api
  * error handling can be better, move away from generic js/Error.
  * gcl node-object-stream support
  * object streams, duplex streams, transform streams
  * support other runtimes? JSC, nashorn
  * sugar for native streams, duplex + transform too
  * https://github.com/Raynes/fs
  * Iequiv should check path AND get-type impl

  * ###### java URL has unique set of methods that should be extended to goog.Uri
    * openStream => opens connection to this URL and returns an input stream for reading
      https://docs.oracle.com/javase/7/docs/api/java/net/URL.html#openStream()

  * test isFd?

  *slurp + spit encodings are broken

  * delete-file should handle absolute paths, not just file objects

  * file reader needs a read, readline methods.
    * line-seq needs stream, probably must be async (breaking from clj)




## examples to do
* streaming mp3 through websocket
* clojure.data.csv port, m3u example?
* cookbook io examples
* large csv file reading via streamsk
* transit
* encodings
* webcrawler?




### Notes
  * biased towards sync calls,
  * consolidated URL & URI, goog.net.Uri is great
  * polymorphic java constructors (ie java.io.File)
    necessitate alot of indirection
  * default-impl-obj + specify! is a cool pattern


#### slurp
 NOT bufferedFileReader+FileStream as in clojure. Nodejs's streams are created
 asynchronously and would require slurp to return a channel. This uses
 FS.readFileSync, fine for small files. Use FileInputStream for more flexibility




; read [] => int , reads a byte of data from this inputstream
; read [^byteArray b] => int ,  Reads up to b.length bytes of data from this input stream into an array of bytes.
; read [^byteArray b, ^int off, ^int len] => int ,   Reads up to len bytes of data from this input stream into an array of bytes.
