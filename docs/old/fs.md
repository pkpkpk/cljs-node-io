# cljs-node-io.fs
 The fs namespace is a convenience wrapper around the node file system module.

+ all functions will throw immediately if given the wrong type. This is a different class of error vs. an actual IO exception, which may be handled differently

+ Predicates
  - Sync predicates do not on op errors, return false
  - Async predicates return chans that receive false on err. These channels only receive booleans.


+ IO operations
  - Sync IO ops *will* throw, error handling is up to user
    - note this is not transactional... some side effects may have occured despite an error
  - Async IO ops all return chans that receive a vector: either `[err]` or `[err data]` depending on the operation.
    - for successful ops, `err` will be `nil`. This lets you destructure the result and branch on err


 ```clojure
 (let [[err data] (<! afn)]
   (if-not err
     (handle-result data)
     (handle-error err)))
 ```


+ In busy processes, it is <em>strongly encouraged</em> to use the asynchronous versions of these calls. The synchronous versions will block the entire process until they complete--halting all connections.

+ The relative path to a filename can be used. Remember, however, that this path will be relative to <code>process.cwd()</code>.

+ Asynchronous methods have no ordering guarantee...so things like the following are error prone ...It could be that `astat` is run before `arename`

```clojure
(def rc (arename "/tmp/hello" "/tmp/world"))
(def sc (astat "tmp/world"))

(take! rc 
  (fn [[e]]
    (if-not e
      (println "rename completed!")
      (throw res))))

(take! sc 
  (fn [[sterr st]]
    (if-not sterr
      (println (js/JSON.stringify res))
      (throw res))))
```
+ The correct way to do it:

```clojure
(go
  (let [[rerr] (<! (arename "/tmp/hello" "/tmp/world"))]
    (if-not rerr
      (let [[sterr st] (<! (astat "/tmp/world"))]
        (if-not sterr
          (println (js/JSON.stringify st))
          (throw sterr)))
      (throw rerr))))
```

