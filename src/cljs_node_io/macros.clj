(ns cljs-node-io.macros)

(defmacro try-true
  [& exprs]
  `(try
     (do
       ~@exprs
       true)
     (catch ~'js/Error ~'e false)))

(defmacro with-chan*
  [form cb]
  `(let [~'c (~'promise-chan)]
     (~@form ~cb)
     ~'c))

(defmacro with-chan
  "Wraps an async call in a channel. Form arg should be a node interop sexp but
   lacking the trailing callback argument. Return channels always receive vectors,
   either [err] or [err data].
   datafn is an optional function to call on data before its placed into channel"
  ([form]
   (let [cb `(fn [& ~'args] (~'put! ~'c (vec ~'args)))]
     `(with-chan* ~form ~cb)))
  ([form datafn]
   (let [cb `(fn [~'e ~'data] (~'put! ~'c [~'e (~datafn ~'data)]))]
     `(with-chan* ~form ~cb))))

(defmacro with-bool-chan
  "assumes that err means false. Return channels receive booleans only"
  [form]
  (let [cb `(fn [~'e] (~'put! ~'c (if-not ~'e true false)))]
    `(with-chan* ~form ~cb)))

(defmacro go-let
  [bindings & body]
  `(~'go (let ~bindings ~@body)))