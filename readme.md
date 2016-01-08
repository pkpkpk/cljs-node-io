# todo
  * convert tests
  * clj tests rely on correctness of java classes, so mock java
     stuff all needs appropriate tests and full api
  * error handling can be better, move away from generic js/Error.
  * gcl node-object-stream support
  * object streams, duplex streams, transform streams
  * support other runtimes? JSC, nashorn
  * cookbook io examples
  * streaming mp3 through websocket
  * clojure.data.csv port, m3u example?


### Notes
  * biased towards sync calls,
  * consolidated URL & URI, goog.net.Uri is great
  * polymorphic java constructors (ie java.io.File)
    necessitate alot of indirection
  * default-impl-obj + specify! is a cool pattern


;File readstream events:
; open,
; readable
; data
; error
; end
; close




;java.io.FileInputStream API
; available
; close
; finalize
; getChannel
; getFD

; read [] => int , reads a byte of data from this inputstream
; read [^byteArray b] => int ,  Reads up to b.length bytes of data from this input stream into an array of bytes.
; read [^byteArray b, ^int off, ^int len] => int ,   Reads up to len bytes of data from this input stream into an array of bytes.
