
# Readstream (stream.readable)
  + ### __events__
    * ##### __"readable"__ ()
      - When a chunk of data can be read from the stream, it will emit a 'readable' event.
      - In some cases, listening for a 'readable' event will cause some data to be read into the internal buffer from the underlying system, if it hadn't already.
      - Once the internal buffer is drained, a 'readable' event will fire again when more data is available.

        ```javascript
        var readable = getReadableStreamSomehow();
        readable.on('readable', () => {
          // there is some data to read now
        });
        ```      
      - The 'readable' event is not emitted in the "flowing" mode with the sole exception of the last one, on end-of-stream.
      - The 'readable' event indicates that the stream has new information: either new data is available or the end of the stream has been reached. In the former case, stream.read() will return that data. In the latter case, stream.read() will return null. For instance, in the following example, foo.txt is an empty file:

        ```js
        const fs = require('fs');
        var rr = fs.createReadStream('foo.txt');
        rr.on('readable', () => {
          console.log('readable:', rr.read());
        });
        rr.on('end', () => {
          console.log('end');
        });
        ```

          The output of running this script is:

          ```
          $ node test.js
          readable: null
          end
          ```

    * ##### __"data"__ (Buffer|string)
        - Attaching a 'data' event listener to a stream that has not been explicitly paused will switch the stream into flowing mode. Data will then be passed as soon as it is available.
        - If you just want to get all the data out of the stream as fast as possible, this is the best way to do so.    
          ```js
          var readable = getReadableStreamSomehow();
          readable.on('data', (chunk) => {
            console.log('got %d bytes of data', chunk.length);
          });
          ```

    * ##### __"error"__ (e)
      - Emitted if there was an error receiving data.

    * ##### __"end"__ ()
      - This event fires when there will be no more data to read.
      - Note that the 'end' event will not fire unless the data is completely consumed. This can be done by switching into flowing mode, or by calling stream.read() repeatedly until you get to the end.
        ```js
        var readable = getReadableStreamSomehow();
        readable.on('data', (chunk) => {
          console.log('got %d bytes of data', chunk.length);
        });
        readable.on('end', () => {
          console.log('there will be no more data.');
        });
        ```      

    * ##### __"close"__ ()
      - Emitted when the stream and any of its underlying resources (a file descriptor, for example) have been closed. The event indicates that no more events will be emitted, and no further computation will occur.
      - Not all streams will emit the `'close'` event.

    * ##### __"open"__  (fd)
      - Emitted when the ReadStream's file is opened.
      - filestream ONLY
      - if stream is opened with an existing fd, then this event is not emitted.

  + ### __methods__
    - ##### __isPaused__ ()-> bool
      - This method returns whether or not the readable has been explicitly paused by client code (using stream.pause() without a corresponding stream.resume())

        ```js
        (def readable (new stream.Readable))
        (.isPaused readable) // === false
        (.pause readable)
        (.isPaused readable) // === true
        (.resume readable)
        (.isPaused readable) // === false
        ```

    - ##### __pause__ ()->this
      - This method will cause a stream in flowing mode to stop emitting 'data' events, switching out of flowing mode. Any data that becomes available will remain in the internal buffer.

        ```js
        (.on readable 'data',
          (fn [chunk]
            (println "got" (.-length chunk) "bytes of data")
            (.pause readable)
            (println 'there will be no more data for 1 second')
            (js/setTimeout
              (fn []
                (println "now data will start flowing again")
                (.resume readable))
              1000)))
        ```

    - ##### __pipe__  (dest, ?opts) -> dest stream
      - This method pulls all the data out of a readable stream, and writes it to the supplied destination, automatically managing the flow so that the destination is not overwhelmed by a fast readable stream.
      - Multiple destinations can be piped to safely.
      - __dest__: Writable Stream, destination for writing data
      - __opts__:
        - ```:end true```
          - End the writer when the reader ends.
      - returns the destination stream, so you can set up pipe chains like so
        ```js
        (let [r (. fs createReadStream "foo.edn")
              z (. zlib createGzip)
              w (. fs createWriteStream "foo.edn.gz")]
          (-> r
            (.pipe z)
            (.pipe w)))
        ```
      - By default stream.end() is called on the destination when the source stream emits 'end', so that destination is no longer writable. Pass ```:end false``` an opt to keep the destination stream open.
        ```js    
        (.pipe reader writer {:end false})
        (.on reader "end" (fn [] (.end writer "Goodbye!\n")))
        ```
      - emulate UNIX cat:
            process.stdin.pipe(process.stdout);
      - Note that process.stderr and process.stdout are never closed until the process exits, regardless of the specified options.

    - ##### __read__  ( ^int ?size) -> str|buffer|nil
      - size: Optional argument to specify how much data to read.
      - The `read()` method pulls some data out of the internal buffer and
      returns it.
      - If there is no data available, then it will return
     `null`.
      - If you do not specify a `size` argument, then it will return all the
     data in the internal buffer.
      - If you pass in a `size` argument, then it will return that many
     bytes. If `size` bytes are not available, then it will return `null`,
     unless we've ended, in which case it will return the data remaining
     in the buffer.
      - If this method returns a data chunk, then it will also trigger the
      emission of a `'data'` event.
      - Note that calling [`stream.read([size])`][stream-read] after the [`'end'`][] event has been triggered will return `null`. No runtime error will be raised.      
      - This method should only be called in paused mode. In flowing mode,
      this method is called automatically until the internal buffer is
      drained.
        ```js
        (.on r "readable"
          (fn []
            (let [chunks (take-while identity (repeatedly  #(.read r 1)))]
              (doseq [chunk chunks]
                (println "got " (.-length chunk) " bytes of data" )))))
        ```

    - ##### __resume__ () -> this
      - This method will cause the readable stream to resume emitting [`'data'`][] events.
      - __This method will switch the stream into flowing mode.__ If you do *not* want to consume the data from a stream, but you *do* want to get to its [`'end'`][] event, you can call [`stream.resume()`][stream-resume] to open the flow of data.

        ```js
        ...
        (.on r "end"
          (fn [] (println "got to the end, but did not read anything"))
        ...
        (.resume r)
        ```      

    - ##### __setEncoding__ (^string encoding) -> this
      - cause the stream to return strings of the specified encoding instead of Buffer objects.
      - For example, if you do `readable.setEncoding('utf8')`, then the output data will be interpreted as UTF-8 data, and returned as strings. If you do `readable.setEncoding('hex')`, then the data will be encoded in hexadecimal string format.
      - This properly handles multi-byte characters that would otherwise be potentially mangled if you simply pulled the Buffers directly and called [`buf.toString(encoding)`][] on them. If you want to read the data as strings, always use this method.
        ```js
        (.setEncoding r "utf8")
        (.on r "data"
          (fn [chunk]
            (assert (string? (type chunk)))
            (println "got " (.-length chunk) " characters of string data")))
        ```

    - ##### __unpipe__ (?dest)
      - This method will remove the hooks set up for a previous [`stream.pipe()`][]
      call.
      - dest refers to optional specific stream to unpipe.
      - If the destination is not specified, then all pipes are removed.
      - If the destination is specified, but no pipe is set up for it, then this is a no-op.
        ```js
        (def readable (getReadableStreamSomehow))
        (def writable (. fs createWriteStream "file.txt")
        // All the data from readable goes into 'file.txt',
        // but only for the first second
        (.pipe readable writable)
        (js/setTimeout
          (fn []
            (println 'stop writing to file.txt')
            (.unpipe readable writable)
            (println 'manually close the file stream')
            (.end writable))
          1000)
        ```

    - ##### __unshift__ (chunk)
      - chunk: Buffer|String, Chunk of data to unshift onto the read queue
      - This is useful in certain cases where a stream is being consumed by a parser, which needs to "un-consume" some data that it has optimistically pulled out of the source, so that the stream can be passed on to some other party.
      - Note that `stream.unshift(chunk)` cannot be called after the [`'end'`][] event has been triggered; a runtime error will be raised.
      - Example:
        1. Pull off a header delimited by \n\n
        2. use unshift() if we get too much
        3. Call the callback with (error, header, stream)
        ```js
          const StringDecoder = require('string_decoder').StringDecoder;
          function parseHeader(stream, callback) {
            stream.on('error', callback);
            stream.on('readable', onReadable);
            var decoder = new StringDecoder('utf8');
            var header = '';
            function onReadable() {
              var chunk;
              while (null !== (chunk = stream.read())) {
                var str = decoder.write(chunk);
                if (str.match(/\n\n/)) {
                  // found the header boundary
                  var split = str.split(/\n\n/);
                  header += split.shift();
                  var remaining = split.join('\n\n');
                  var buf = new Buffer(remaining, 'utf8');
                  if (buf.length)
                    stream.unshift(buf);
                  stream.removeListener('error', callback);
                  stream.removeListener('readable', onReadable);
                  // now the body of the message can be read from the stream.
                  callback(null, header, stream);
                } else {
                  // still reading the header.
                  header += str;
                }
              }
            }
          }
        ```
      - Note that, unlike [`stream.push(chunk)`][stream-push], `stream.unshift(chunk)`
      will not end the reading process by resetting the internal reading state of the
      stream. This can cause unexpected results if `unshift()` is called during a
      read (i.e. from within a [`stream._read()`][stream-_read] implementation on a
      custom stream). Following the call to `unshift()` with an immediate
      [`stream.push('')`][stream-push] will reset the reading state appropriately,
      however it is best to simply avoid calling `unshift()` while in the process of
      performing a read.


  + ### __Properties__
    - ##### __path__
      - The path to the file the stream is reading from.

<hr>
## FileInputStream
  + ```(FileInputStream. fileable {opts} ) ``` -> ch
  + Be aware that, unlike the default value set for highWaterMark on a readable stream (16 kb), the stream returned by this method has a default value of 64 kb for the same parameter.
  + __options__
    - options can include `start` and `end` values to read a range of bytes from the file instead of the entire file.
      - Both are inclusive and start at 0.
    - The encoding can be any one of those accepted by Buffer.
    - If fd is specified, ReadStream will ignore the path argument and will use the specified file descriptor.
      - this means no 'open' event is emitted
      - fd should be blocking; non-blocking fds should be passed to net.Socket.
    - If ```:autoClose false```, then the file descriptor won't be closed, even if there's an error. It is your responsibility to close it and make sure there's no file descriptor leak.
    - If ```:autoClose true``` (default behavior), on 'error' or 'end' the file descriptor will be closed automatically.
    - ```:mode``` sets the file mode (permission and sticky bits), but only if the file was created.
    - __defaults:__
      - ```:flags 'r' ```
      - ```:encoding nil ``` ???!!!!
      - ```:fd nil```
      - ```:mode 0o666```???!!!!!
      - ```:autoClose true```
      - ```:start ???``` ?????
      - ```:end ???``` ?????      
  + examples
    - read the last 10 bytes of a file which is 100 bytes long:

        ```clj
        (FileInputStream.  'sample.txt' {start: 90, end: 99})
        ```






<hr>



```clj
(defn slurp-stream
   [f]
   (let [sb  (StringBuffer.)
         r   (apply reader f nil)
         res (atom nil)] ; channel?
     (doto r
       (.on "error" (fn [e] (throw e)))
       (.on "readable"
          (fn []
            (loop [chunk (.read r 1)]
              (if (nil? chunk)
                (reset! res (.toString sb))
                (do
                  (.append sb chunk)
                  (recur (.read r)))))))) res))
```

; read [] -> int , reads a byte of data from this inputstream
; read [^byteArray b] -> int ,  Reads up to b.length bytes of data from this input stream into an array of bytes.
; read [^byteArray b, ^int off, ^int len] -> int ,   Reads up to len bytes of data from this input stream into an array of bytes.
