# todo
  * convert tests
   * fs.stats wrapper in util ns
  * file copying
  * sockets
  * clj tests rely on correctness of java classes, so mock java
     stuff all needs appropriate tests and full api
  * error handling can be better, move away from generic js/Error.
  * gcl node-object-stream support
  * object streams, duplex streams, transform streams
  * support other runtimes? JSC, nashorn
  * sugar for native streams, duplex + transform too
  * ###### https://github.com/Raynes/fs

  * ###### java URL has unique set of methods that should be extended to goog.Uri
    * openStream => opens connection to this URL and returns an input stream for reading
      https://docs.oracle.com/javase/7/docs/api/java/net/URL.html#openStream()


## examples to do
* streaming mp3 through websocket
* clojure.data.csv port, m3u example?
* cookbook io examples
* large csv file reading via streamsk
* transit




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
