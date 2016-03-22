### Buffering

  Both Writable and Readable streams will buffer data on an internal
  object which can be retrieved from `_writableState.getBuffer()` or
  `_readableState.buffer`, respectively.

  The amount of data that will potentially be buffered depends on the
  `highWaterMark` option which is passed into the constructor.

  Buffering in Readable streams happens when the implementation calls
  `stream.push(chunk)`. If the consumer of the Stream does not
  call `stream.read()`, then the data will sit in the internal
  queue until it is consumed.

  Buffering in Writable streams happens when the user calls
  `stream.write(chunk)` repeatedly, even when it returns `false`.

  The purpose of streams, especially with the `stream.pipe()` method, is to
  limit the buffering of data to acceptable levels, so that sources and
  destinations of varying speed will not overwhelm the available memory.

<hr>

###  cljs-node-io.streams/FileInputStream
#### `(FileInputStream. path ?options ) ` -> ReadableStream
  + wrapper around `fs.createReadStream`
  + Be aware that, unlike the default value set for highWaterMark on a readable stream (16 kb), the stream returned by this method has a default value of 64 kb for the same parameter.
  + options can include `start` and `end` values to read a range of bytes from the file instead of the entire file.
    - Both are inclusive and start at 0.
  + `path` : string | Uri | File
  + `options` : optional map
    - `:flags` ^string `"r"`
    - `:encoding` ^string `"utf8"`
      - The encoding can be any one of those accepted by Buffer.
      - use `""` to return raw buffers instead of strings
    - `:fd` ^int `nil`
      - If fd is specified, path arg is ignored
      - fd should be blocking; non-blocking fds should be passed to net.Socket.
    - `:mode` ^int `0o666`
      - sets the file mode (permission and sticky bits), but only if the file was created.
    - `:autoClose` ^bool `true`
      - If true (default behavior), on 'error' or 'end' the file descriptor will be closed automatically.
      - If false, then the file descriptor won't be closed, even if there's an error. It is your responsibility to close it and make sure there's no file descriptor leak.
    - `:start` ^int 0
      - index of first byte to read
    - `:end` ^int Infinity
      - index of last byte to read

  #### methods
    - __getFd__ ( ) -> int
      - baked in listener for `"open"`, returns file-descriptor
      - if you opened the stream with an existing fd this method returns nil            

  #### Properties
    - __path__ -> string
      - The path to the file the stream is reading from.
      - if you opened the stream with an existiny fd this property is nil      

  #### example: read the last 10 bytes of a file which is 100 bytes long:
      ```clj
      (FileInputStream.  'sample.txt' {start: 90, end: 99})
      ```

<hr>

###  cljs-node-io.streams/FileOutputStream
#### `(FileOutputStream. path ?options ) ` -> WritableStream
  + A wrapper around `fs.createWriteStream`
  + Note: opening a nonexisting file creates an empty file even before writing
  + `path` : string | Uri | File
  + `options` : optional map
    - `:append` : ^boolean `false`
      - sets as `:flags to "a"`
      - is overridden by *any* passed flag value
    - `:flags` ^string `"w"`
      - any passed value overrides `:append true`
    - `:defaultEncoding` ^string `"utf8"`
      - can be any encoding accepted by Buffer
    - `:fd` ^int `nil`
      - if specified path arg is ignored
      - fd should be blocking; non-blocking fds should be passed to net.Socket
    - `:start` ^int 0
      - byte index to start writing at
    - `:autoClose` ^boolean `true`
      - If true (default behavior), on 'error' or 'end' the file descriptor will be closed automatically.
      - If false, then the file descriptor won't be closed, even if there's an error. It is your responsibility to close it and make sure there's no file descriptor leak.    
    - `:mode` ^int `0o666`
      - sets the file mode (permission and sticky bits), but only if the file was created.

  #### methods
    - __getFd__ ( ) -> int
      - baked in listener for `"open"` event, returns file-descriptor
      - if you opened the stream with an existing fd this method returns nil      

  #### properties
    * __path__ -> string
      - The path to the file the stream is reading from.
      - if you opened the stream with an existiny fd this property is nil
    * __bytesWritten__
      - The number of bytes written so far. Does not include data that is still queued for writing.


<hr>

###  cljs-node-io.streams/DuplexStream
#### `(DuplexStream {options})`
  + Examples:
    - TCP sockets
    - zlib streams
    - crypto streams
  + A wrapper around the stream.Duplex constructor. Duplex streams are streams that implement both the Readable and Writable interfaces.
    - `stream.Duplex` is an abstract class designed to be extended with an underlying implementation of the `stream._read(size)` and `stream._write(chunk, encoding, callback)` methods as you would with a Readable or Writable stream class.
    - the options object  given to `stream.Duplex` is passed to both the Readable & Writable constructors

  ##### Options : map
    - `:read` : fn
      - *required*, see implementing Readable streams
    - `:write` : fn
      - *required*, see implementing Writable streams
    - `:allowHalfOpen` : Boolean
      - Default: `true`    
      - If `false`, automatically end the readable side when the Writable side ends & vice versa
    * `:objectMode` : Boolean
      - Default : `false`
      - Both read & write object-stream behavior.
        - `stream.read(n)` returns a single value instead of a Buffer of size n.
        - `stream.write(anyObj)` can write arbitrary data instead of only `Buffer` / `String` data.            
    - `:readableObjectMode` : Boolean
      - Default : `false`    
      - sets objectMode for the readable side
      - is overridden by `:objectMode`
    - `:writableObjectMode` : Boolean
      - Default : `false`    
      - sets objectMode for the readable side
      - is overridden by `:objectMode`

<hr>

###  cljs-node-io.streams/TransformStream
#### `(TransformStream {options})`
  + A wrapper around the stream.Transform constructor
  + A "transform" stream is a duplex stream where the output is causally connected in some way to the input, such as a zlib stream or a crypto stream.
  + There is no requirement that the output be the same size as the input, the same number of chunks, or arrive at the same time.
    - Hash stream will only ever have a single chunk of output which is provided when the input is ended.
    - A zlib stream will produce output that is either much smaller or much larger than its input.
  + Unlike Duplex streams, you dont need to implement `_read` & `_write` methods, but instead you *must implement a `_transform` method* & and an optional `_flush` method
  ##### Options : map
    - `:transform` : fn
      - *Required but do not call directly*
      - Implementation for `stream._transform()`, see below
    - `:flush` : fn
      - Implementation `stream._flush`, see below

  ##### Events
    + `"finish"`
      - same as from Writable
      - fired after `stream.end()` is called & all chunks have been processed by the transform function
    + `"end"`
      - same as from Readable
      - is fired after all data has been output, after the cb in `stream._flush()`

  ##### `stream._flush(cb)`
    + cb : fn (?e)
      - callback to run after done flushing remaining data
    + *implement but do not call this method*
      - to be used by internal transform methods only
    +   In some cases, your transform operation may need to emit a bit more data at the end of the stream.
      - For example, a `Zlib` compression stream will store up some internal state so that it can optimally compress the output. At the end, however, it needs to do the best it can with what is left, so that the data will be complete.
    + `_flush()` is called after all written data is consumed but before emitting the `"end"` event. Call `transform.push(chunk)`  as appropriate and call the cb when complete.

  ##### `stream._transform(chunk, encoding, cb)`
    + `chunk` : Buffer|String
      - the chunk to be transformed
      - always a buffer unless the `:decodeStrings` option was set to `false` (see implementing writables)
    + `encoding` : string
      - if chunk is a buffer this is ignored
    + `cb` : fn
      - call this when done processing the supplied chunk, optionally with err
    +  `_transform()` should do whatever has to be done in this specific Transform class, to handle the bytes being written, and pass them off to the readable portion of the interface. Do asynchronous I/O, process things, and so on.
    + Call the callback function only when the current chunk is completely consumed. Note that there may or may not be output as a result of any particular input chunk.

##### Example: line delimited edn parsestream

```
(spit "foo.ednl"  "{:a 1}\n{:a 2}\n{:a 3}\n")

(defn ednlParseStream []
  (let [sb  (StringBuffer.)
        decoder (new StringDecoder "utf8")
        xf     (fn [chunk enc cb]
                 (this-as this
                   (.append sb (.write decoder chunk))
                   (let [lines (.split (.toString sb) #"\r?\n")
                         _     (.set sb (.pop lines))]
                     (doseq [line lines]
                       (let [obj  (try
                                    (cljs.reader/read-string line)
                                    (catch js/Error e (throw e )))] ;should do something here
                          (.push this obj)))
                     (cb))))
        opts {:readableObjectMode true
              :transform xf}]
    (TransformStream opts)))

(def parser (ednlParseStream))
(def fstream (FileInputStream "foo.ednl"))

(.pipe fstream parser )

(.read parser) ;=> {:a 1}
(.read parser) ;=> {:a 2}
```

<hr>

###  cljs-node-io.streams/BufferStream
#### `(BufferStream buffer ?{options})` -> ReadableStream
  - Creates a ReadableStream from a Buffer.
  - options are same as for ReadableStream except the `:read` fn is provided.
    - If you provide :read, it is ignored

```
; pass "" as encoding to get raw buffer
(def photo (slurp "example.jpeg" :encoding ""))

(defn run-server []
  (let [handler (fn [request response]
                  (.writeHead response 200 "OK"
                              #js{"Content-Type"        "image/jpeg"
                                  "Content-Disposition" "inline; filename=example.jpeg"
                                  "Content-Length"      (.-length photo)})
                  (.pipe (BufferStream photo) response))
        server  (.createServer http handler)]
    (.listen server 8080)
    (println "server running on port 8080")
    server))

(def server (run-server))
```
