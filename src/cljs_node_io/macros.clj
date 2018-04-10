(ns cljs-node-io.macros
  (:require [cljs.compiler :as comp]
            [cljs.env :as env]
            [cljs.analyzer :as ana]
            [clojure.string :as string]))

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

(defmacro goog-typedef
  "Define a custom type for use in JSDoc type annotations.

   docstring can be a simple type expression:
     `(good-typedef my-vector \"{!IVector}\")`

   Or you can use other tags by manually specifying a typedef tag:
     `(good-typedef my-string-array \"@typedef {!Array<string>}
                                      @implements {SomeProtocol}\")`

   Each annotation must occur on its own line with a space separating the tag
   and its type-expression"
  [sym docstring]
  ; (assert-args goog-typedef (core/string? docstring))
  (when (#{:error :warning} (get-in @env/*compiler* [:options :closure-warnings :check-types]))
    (let [typename (comp/munge (str *ns* "/" sym))
               docstring (if (string/starts-with? docstring "@typedef")
                           docstring
                           (str "@typedef{" docstring "}"))]
      `(do
         (declare ~(vary-meta sym assoc :tag 'symbol :typedef true))
         (~'js* ~(str "/** " docstring " */"))
         (~'js* ~(str typename))))))