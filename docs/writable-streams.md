# WriteStream (stream.writable)
The Writable stream interface is an abstraction for a *destination*
that you are writing data *to*.

Examples of writable streams include:

* [HTTP requests, on the client][]
* [HTTP responses, on the server][]
* [fs write streams][]
* [zlib streams][zlib]
* [crypto streams][crypto]
* [TCP sockets][]
* [child process stdin][]
* [`process.stdout`][], [`process.stderr`][]


+ ### events

  - ##### "drain" ()
    - if a `stream.write(chunk)` call returns false, then the "drain" event will indicate when it is appropriateto begin writing more data to the stream

      ```clj
      (defn writeOneMillionTimes
        [writer data encoding cb]
        (let [i     (atom 1000000)
              ok    (atom true)
              write (fn write []
                      (while (and (> @i 0) @ok) ;bails if last write failed
                        (do
                          (swap! i dec)
                          (if (zero? @i)
                            ; i=0 so we write the last time!
                            (.write writer data encoding cb)
                            ; else contine or wait,
                            ; no cb since we are not done yet
                            (reset! ok (.write writer data encoding)))))
                      (if (> i 0) ;resume writing after drain event
                        (.once writer "drain" write )))]
          (write)))

      ```

  - ##### "error" (e)
    - Emitted if there was an error when writing or piping data.

  - ##### "finish"
    - When the [`stream.end()`][stream-end] method has been called, and all data has
    been flushed to the underlying system, this event is emitted.

      ```js
      (.on writer "finish"
        (fn [] (println "all writes are now complete")))

      (dotimes [i 100]
        (.write writer (str "hello #" i "!")))

      (.end writer "this is the end\n")
      ```

  - ##### "pipe" (src)
    - src: {stream.Readable} source stream that is piping to this writable
    - emitted whenever the pipe method is called on as readable stream, adding this writable to its set of destinations
      ```js
      (. writer on "pipe"
        (fn [src]
          (println "something is piping into the writer")
          (assert (= src reader))))

      (-> reader
        (.pipe writer))
      ```

  - ##### "unpipe" (src)
    - src: the stream on whic unpipe was called, removing the writer as a destination
      ```js
      (. writer on "unpipe"
        (fn [src]
          (println "something has stopped piping into the writer")
          (assert (= reader src))))

      (.pipe reader writer)
      (.unpipe reader writer)
      ```

  - ##### "open" (fd)
    - filestream only
    - fd: file descriptor for the file being opened


+ ### methods

  - ##### cork
    - forces buffering of all writes
