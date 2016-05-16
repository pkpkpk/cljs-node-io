(ns cljs-node-io.macros)

(defmacro try-true
  [& exprs]
  `(try
     (do
       ~@exprs
       true)
     (catch ~'js/Error ~'e false)))


(defmacro with-chan
  ([form]
   (let [cb `((fn [& ~'args] (~'put! ~'c (apply vector ~'args))))]
     `(let [~'c (~'chan)]
        ~(concat form cb)
        ~'c)))
  ([form datafn]
   (let [cb `((fn [~'e ~'data] (~'put! ~'c [~'e (~datafn ~'data)])))]
     `(let [~'c (~'chan)]
        ~(concat form cb)
        ~'c))))


