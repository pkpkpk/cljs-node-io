(ns  cljs-node-io.protocols)

(defprotocol Coercions
  "Coerce between various 'resource-namish' things."
  (as-file [x] "Coerce argument to a file.")
  (as-url [x] "Coerce argument to a URL."))


(defprotocol IOFactory
  "Factory functions that create various node I/O stream types, on top of anything that can
   be unequivocally converted to the requested kind of stream.
   Common options include
     :encoding  string name of encoding to use, e.g. \"UTF-8\".
   Callers should generally prefer the higher level API provided by
   reader, writer, input-stream, and output-stream."
  (make-reader [x opts] "Defers back to the InputStream")
  (make-writer [x opts] "Defers back to the OutputStream")
  (make-input-stream [x opts] "Creates a buffered InputStream. See also IOFactory docs.")
  (make-output-stream [x opts] "Creates a buffered OutputStream. See also IOFactory docs."))

(defprotocol IFile
  "A marker protocol indicating a reified File")

(defprotocol IInputStream
  "A marker protocol indicating an input-stream")

(defprotocol IOutputStream
  "A marker protocol indicating an output-stream")

(defprotocol IChildProcess "A marker protocol indicating an ChildProcess")