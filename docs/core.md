# cljs-node-io.core

* ### delete-file
  - `(delete-file f & [silently])`
  - Delete file f. Raise an exception if it fails unless silently is true.


* ### file
  - `(file arg)`
  - `(file parent child)`
  - `(file parent child & more)`
  - Returns a reified File, passing each arg to as-file.  Multiple-arg versions treat the first argument as parent and subsequent args as children relative to the parent.
  - see File API documentation


* ### make-parents
  - `(make-parents f & more)`
  - Given the same arg(s) as for file, creates all parent directories of the file they represent.


* ### as-relative-path
  - `(as-relative-path x) `
  - Take an as-file-able thing and return a string if it is a relative path, else IllegalArgumentException.

* ### copy
  * `(copy input output & opts)` -> nil | throws
  - Copies input to output.  Returns nil or throws IOException.
  - Input may be an InputStream, Reader, File, byte[], or String.
  - Output may be an OutputStream, Writer, or File.
  - Does not close any streams except those it opens itself (on a File)
  - opts : k/v pairs :
    - `:buffer-size` : int
      - buffer size to use, default is 1024.
    - `:encoding` : string
      - encoding to use if converting between byte and char streams.


+ ### spit
  - Opposite of slurp.  Opens f with writer, writes content. Options passed to a file/file-writer.
  - `(spit f content & opts)`
   * content : `string` | `Buffer`
     - if buffer, encoding is ignored
      - __you are responsible for feeding spit an appropriate string representation for non-clj objects.__
          - To print json you must use `(js/JSON.stringify js-data ...)`
          - clj data-structures print fine automatically, use sslurp/spit to read & write edn data.
   * returns nil
   * default opts:
      - `:append false`
      - `:encoding "utf8"`
      - `:mode  436`
      - `:flags "w"`
      - `:async? false`
      - ~~:stream? false~~
    * flags take precedence over append        
  - defaults to synchronous write, use *aspit* for *async* writes


+ ### aspit
  - asynchronous version of spit
  - `(aspit f content & opts)`
    + content : `string` | `Buffer`
      - if buffer, encoding is ignored
    + returns channel receiving `true` | `error`
  - default opts:
    - `:append false`
    - `:encoding "utf8"`
    - `:mode  436`
    - `:flags "w"`
    - `:async? true`
    - ~~:stream? false~~
  - flags take precedence over append
  - Note that it is unsafe to use fs.writeFile multiple times on the same file without waiting for the callback. For this scenario, `FileOutputStream` is strongly recommended.


* ### slurp
  -  opens a reader on f and returns its contents. Returns a string synchronously by default
  - `(slurp f & opts)`
    * default opts:
      - `:encoding "utf8"`
      - `:flags "r"`
      - `:async? false`
      - ~~:stream? false~~
    * If `:encoding ""` (an explicit empty string), returns raw buffer instead of string.
    * If `:async? true`, returns channel which will receive err|data specified by encoding via put! cb



+ ### aslurp
  - async slurp, returns a chan to receive err|data
  - `(slurp f & opts)`
    - default opts:
      - `:encoding "utf8"`
      - `:flags "r"`
      - `:async? true`
      - ~~:stream? false~~
    - If `:encoding ""` (an explicit empty string), ch receives raw buffer instead of string.


+ ### sslurp
  - *super* slurp, convenience over slurp
  - automatically reads edn+json file into clj data-structures


+ ### saslurp
  - super aslurp


+ ### file-seq
  - A lazy tree seq on files in a directory



+ ### error?
  + `(error? Object)` -> Boolean
  - a predicate fn that checks if an object is an instance of js/Error
