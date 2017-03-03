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
  - Input may be an `InputStream`, `File`, `buffer`, or `string` where string is interpreted as a file path
  - Output may be an `OutputStream`, `File`, or `string` where string is interpreted as a file path
  - opts : k/v pairs :
    - `:encoding` : `string`
      - encoding to write withq

* ### slurp :`(slurp f & opts)`
  + Returns a string synchronously. Unlike JVM, does not use FileInputStream.
  + __f__ : `string`|`File`
  + Only option at this time is :encoding
    - If `:encoding ""` (an explicit empty string), returns raw buffer instead of string.
  + __Return__: `string`|`Buffer`

+ ### aslurp : `(aslurp f & opts)`
  + asynchronous slurp
  + __f__: `string` | `File`
  + __default opts__:
      - `:encoding "utf8"`
        - If `:encoding ""` (an explicit empty string), returns raw buffer instead of string.
  + __Return__ : `Channel<[err data]>`
   - a 2 element vector where err is nil when data is successfully read

+ ### spit : `(spit f content & opts)`
  + synchronously writes content to filepath
  + __f__: `string`|`File`
  + __content__ : `string` | `Buffer`
    - if buffer, encoding is ignored
    - __you are responsible for feeding spit an appropriate string representation for non-clj objects.__
        - To print json you must use `(js/JSON.stringify js-data ...)`
  + __default opts__:
    - `:append false`
      - overridden by explicitly set flags
    - `:encoding "utf8"`
    - `:mode  436`
    - `:flags "w"`
  + __Return__: `nil` or throws

+ ### aspit :`(aspit f content & opts)`
  + asynchronous spit
  + __f__ : `string`| `File`
  + __content__ : `string` | `Buffer`
  + __default opts__:
    - `:append false`
      - overridden by explicitly set flags
    - `:encoding "utf8"`
      - if content is a buffer, encoding is ignored
    - `:mode  436`
    - `:flags "w"`
  + __Return__ `Channel<[?err]>`
   - a single element vector containing error object or nil on success
  + Note that it is unsafe to use fs.writeFile multiple times on the same file without waiting for the callback. For this scenario, `FileOutputStream` is strongly recommended.


<hr>
## Predicates

+ ### Error? : `(Error? obj)` -> `boolean`
  - a predicate fn that checks if an object is an instance of js/Error

+ ### Buffer? : `(Buffer? obj)` -> `boolean`
  - sugar over Buffer.isBuffer

+ ### input-stream? : `(input-stream? obj)` -> `boolean`
  - checks if an object implements IInputStream

+ ### output-stream? : `(output-stream? obj)` -> `boolean`
  - checks if an object implements IOutputStream