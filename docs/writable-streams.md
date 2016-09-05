# WriteStream (stream.writable)
  + The Writable stream interface is an abstraction for a *destination* that you are writing data *to*.
  + Examples of writable streams include:
    * HTTP requests, on the client
    * HTTP responses, on the server
    * *FileOutputStream* (fs write streams)
    * zlib streams
    * crypto streams
    * TCP sockets
    * child process stdin
    * `process.stdout`, `process.stderr`

+ ### events

  - #### "drain" ()
    - if a `stream.write(chunk)` call returns false, then the "drain" event will indicate when it is appropriate to begin writing more data to the stream

      ```clojure
      (defn writeOneMillionTimes
        [writer data encoding cb]
        (let [i     (atom 1000000)
              ok    (atom true)
              write (fn write []
                      (while (and (> @i 0) @ok) ;bails if last write failed
                        (swap! i dec)
                        (if (zero? @i)
                          ; i=0 so we write the last time!
                          (.write writer data encoding cb)
                          ; no cb since we are not done yet
                          (reset! ok (.write writer data encoding))))
                      (if (> i 0) ;resume writing after drain event
                        (.once writer "drain" write )))]
          (write)))

      ```

  - #### "error" (e)
    - Emitted if there was an error when writing or piping data.

  - #### "finish"
    - When the `stream.end()` method has been called, and all data has
    been flushed to the underlying system, this event is emitted.

      ```clojure
      (.on writer "finish" (fn [] (println "all writes are now complete")))

      (dotimes [i 100]
        (.write writer (str "hello #" i "!")))

      (.end writer "this is the end\n")
      ```

  - #### "pipe" (src)
    - `src` : {stream.Readable}
      - source stream that is piping to this writable
    - emitted whenever the pipe method is called on as readable stream, adding this writable to its set of destinations
      ```clojure
      (. writer on "pipe"
        (fn [src]
          (println "something is piping into the writer")
          (assert (= src reader))))

      (-> reader
        (.pipe writer))
      ```

  - #### "unpipe" (src)
    - src: the stream on whic unpipe was called, removing the writer as a destination
      ```clojure
      (. writer on "unpipe"
        (fn [src]
          (println "something has stopped piping into the writer")
          (assert (= reader src))))

      (.pipe reader writer)
      (.unpipe reader writer)
      ```

+ ### methods

  - #### cork ()
    - forces buffering of all writes
    - Buffered data will be flushed either at [`stream.uncork()`][] or at
      [`stream.end()`][stream-end] call.

  - #### uncork ()
    - Flush all data buffered since stream.cork call

  - #### end (?chunk ?encoding ?cb )
    - __chunk__: `{string|buffer.Buffer}`
    - call this method when there is no more data to be written to the stream.
    - If cb, cb is attached as a listener for "finish"
    - calling `steam.write` after `stream.end`  will raise an error

      ```clojure
      (def file (. fs createWriteStream "example.txt"))
      (. file write "hello, ")
      (. file end "world!")
      ; no more writing is allowed
      ```

  - #### setDefaultEncoding (encoding)
    - sets the default encoding for a writable stream

  - #### write (chunk ?encoding ?cb) -> Boolean
    - This method writes some data to the underlying system, and calls the supplied callback once the data has been fully handled.
    - `chunk`: string|Buffer
    - `encoding` : string : encoding when chunk is a string
    - Return: Boolean
      - `true` if the data was handled completely, keep writing!
      - `false` the data was buffered internally
       - you can keep writing, but it eats up memory so be cautious.
          - wait for `"drain"`


<hr>


# Implementing Writable Streams :

### cljs-node-io.streams/WritableStream
#### `(WritableStream options)`
  - a wrapper around stream.Writable that calls its constructor so that buffering settings are properly initialized
  * `options` : `{IMap}`
    * __:highWaterMark__ :`{number}`
      - Int
      - Buffer level when `stream.write()` starts returning `false`
      - Default = `16384` (16kb), or `16` for `objectMode` streams.
    * __:decodeStrings__ : `{boolean}`
      - Whether or not to decode strings into Buffers before passing them to `stream._write()`
      - Default : `true`
    * __:objectMode__ : `{boolean}`
      - Whether or not the `stream.write(anyObj)` is a valid operation.
      - If set you can write arbitrary data instead of only `Buffer` / `String` data.
      - Default : `false`
    * __:write__ : `Function`
      - implementation for stream.\_write() (see below)
      - *Required*
    * __:writev__ : `Function`
      - Implementation for stream.\_writev() (see below)

#### writable.\_write(chunk, encoding, callback)
  * __chunk__ : `{buffer.Buffer|string}`
    - the chunk to be writter
    - will always be a buffer unless `:decodeStrings false`
  * __encoding__ : `{string}`
    - If chunk is a string, then use this encodings
    - If chunk is a buffer, then encoding is ignored
  * __callback__ : `{function}`
    - call this optionally w/ an error when you are done processing the supplied channel
  * all writable stream implementations must provide a `stream._write` method to send data to the underlying resource.
    - it must *not* be called directly, is used by internals
  * call the callback using the standard cb(err) to signal write completed successfully or with an error.
  * if `decodeStrings true` is set at class construction, then `chunk` arg can be a string and not a Buffer, relying on what you pass as `encoding`

#### writable.\_writev(chunks, cb)
  * __chunks__ : `Array`
    - chunks to be written, where each chunk has format:
      `#js{"chunk" .... , "encoding" ...}`
  * this function is optional. If implemented, is called with all the chunks that are buffered in the write queue
  * must not be called directly
