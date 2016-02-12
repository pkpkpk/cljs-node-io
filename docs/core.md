# cljs-node-io.core

  * __delete-file__
    - ```(delete-file f & [silently])```
    - Delete file f. Raise an exception if it fails unless silently is true.


  * __file__
    - ```(file arg)```
    - ```(file parent child)```
    - ```(file parent child & more)```
    - Returns a reified File, passing each arg to as-file.  Multiple-arg versions treat the first argument as parent and subsequent args as children relative to the parent.      


  * __make-parents__
    - ```(make-parents f & more)```
    - Given the same arg(s) as for file, creates all parent directories of the file they represent.


  * __as-relative-path__
    - ```(as-relative-path x) ```
    - Take an as-file-able thing and return a string if it is a relative path, else IllegalArgumentException.


  * __spit__
    - Opposite of slurp.  Opens f with writer, writes content. Options passed to a file/file-writer.
    - ```(spit f content & opts)```
     * content : ```string``` | ```Buffer```
       - if buffer, encoding is ignored     
     * returns nil
     * default opts:
        - ```:append false```
        - ```:encoding "utf8"```
        - ```:flags "w"```
        - ```:async? false```
        - ~~:stream? false~~
      * flags take precedence over append        
    - defaults to synchronous write, use *aspit* for *a*sync writes


  * __aspit__
    - asynchronous version of spit
    - ```(aspit f content & opts)```
     * content : ```string``` | ```Buffer```
       - if buffer, encoding is ignored
     * returns channel receiving ```true``` | ```error```
     * default opts:
        - ```:append false```
        - ```:encoding "utf8"```
        - ```:flags "w"```
        - ```:async? true```
        - ~~:stream? false~~
      * flags take precedence over append
    - Note that it is unsafe to use fs.writeFile multiple times on the same file without waiting for the callback. For this scenario, fs.createWriteStream is strongly recommended.


  * __slurp__
    -  opens a reader on f and returns its contents. Returns a string synchronously by default
    - ```(slurp f & opts)```
      * default opts:
        - ```:encoding "utf8"```
        - ```:flags "r"```
        - ```:async? false```
        - ~~:stream? false~~
      * If ```:encoding ""``` (an explicit empty string), returns raw buffer instead of string.
      * If ```:async? true```, returns channel which will receive err|data specified by encoding via put! cb



  * __aslurp__
    - async slurp, returns a chan to receive err|data
    - ```(slurp f & opts)```
      * default opts:
        - ```:encoding "utf8"```
        - ```:flags "r"```
        - ```:async? true```
        - ~~:stream? false~~
      * If ```:encoding ""``` (an explicit empty string), ch receives raw buffer instead of string.


  * __sslurp__
    - *super* slurp, convenience over slurp
    - automatically reads edn+json file into clj data-structures


  * __saslurp__
    - *s*uper *a*sync *slurp*


  *__file-seq__
    - A lazy tree seq on files in a directory
