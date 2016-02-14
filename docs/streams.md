
### Generic Readstream events:
 * readable
 * data
 * error
 * end
 * close




### File readstream events:
 * open    _(fd)->_


(defn slurp-stream??
  "Opens a reader on f and reads all its contents, returning a string.
  See clojure.java.io/reader for a complete list of supported arguments."
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


      ; (renameTo [_]) ;=> boolean
      ; (setLastModified [_]) ;=> boolean
      ; (setReadOnly [_]) ;=> boolean


; read [] -> int , reads a byte of data from this inputstream
; read [^byteArray b] -> int ,  Reads up to b.length bytes of data from this input stream into an array of bytes.
; read [^byteArray b, ^int off, ^int len] -> int ,   Reads up to len bytes of data from this input stream into an array of bytes.
