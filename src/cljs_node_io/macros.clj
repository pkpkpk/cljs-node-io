(ns cljs-node-io.macros
   (:refer-clojure :exclude [with-open]))

;https://github.com/markmandel/while-let
(defmacro while-let
  "Repeatedly executes body while test expression is true, evaluating the body with binding-form bound to the value of test."
  [[form test] & body]
  `(loop [~form ~test]
       (when ~form
           ~@body
           (recur ~test))))

(defmacro try-true
  [expr]
  `(try
     (do
       ~expr
       true)
     (catch ~'js/Error ~'e false)))