(ns cljs-node-io.test.file
  (:require [cljs.test :refer-macros [deftest is testing run-tests are]]
            [clojure.string :as s :refer [starts-with?]]
            [cljs-node-io.file :refer [File createTempFile setReadable* setWritable*]]
            [cljs-node-io.protocols :refer [Coercions as-file as-url ]]
            [cljs-node-io.core :refer [spit slurp]]))


; test delete & empty v full directories

(deftest test-createNewFile-and-delete
  (let [f (createTempFile "foo" ".txt")]
    (is (= false (.exists f)))
    (is (= 0     (.length f)))
    (is (= true  (.createNewFile f)))
    (is (= false (zero? (.length f))))
    (is (= true  (.exists f)))
    (is (= nil   (.list f)))
    (is (= false (.createNewFile f)))
    (is (= true  (.delete f)))
    (is (= false (.exists f)))
    (is (= false (.delete f)))))


(deftest test-directory-functions
  (let [d  (createTempFile "TEST_DIRECTORY" "")
        d2 (createTempFile "subdir" "" d)
        d3 (createTempFile "subdir2" "" d2)
        foo  (createTempFile "foo" nil d)
        bar  (createTempFile "bar" nil d)
        deep  (createTempFile "deep" nil d3)]
    (.delete d)
    ; create & delete empty directory
    (is (= false (.exists d)))
    (is (= true  (.mkdir d)))
    (is (= true  (.exists d)))
    (is (= true  (.isDirectory d)))
    (is (= true  (empty? (.list d))))
    (is (= true  (.delete d)))
    (is (= false (.exists d)))
    (is (= false (.isDirectory d)))
    ; dir with a file & subdir siblings
    (is (= true (.mkdir d)))
    (is (= true (.createNewFile foo)))
    (is (= true (.createNewFile bar)))
    (is (= true (.mkdir d2)))
    (is (= ["bar.tmp" "foo.tmp" "subdir"] (.list d)))
    (is (= ["bar.tmp" "foo.tmp"] (.listFiles d)))
    (is (= ["foo.tmp"] (.list d (fn [d name] (starts-with? name "foo") ) )))
    (is (= ["bar.tmp" "subdir"] (.list d (fn [d name] (not (starts-with? name "foo")) ) )))
    (is (= ["bar.tmp"] (.listFiles d (fn [d name] (not (starts-with? name "foo")) ) )))
    (is (= false (.delete d)))
    (is (= true (.exists foo)))
    (is (every? true? (mapv #(.delete %) (list foo bar d2 d))))
    ;derive parent dirs from subdir via mkdir
    (is (every? false? (mapv #(.exists %) (list deep d3 d2 d))))
    (is true (.mkdirs deep)) ; Should make b just like java, which is why core/make-parents exists
    (is (every? true? (mapv #(and (.isDirectory %) (.exists %)) (list deep d3 d2 d))))
    (is (every? true? (mapv #(.delete %) (list deep d3 d2 d))))))

(deftest test-setReadable*
  ; rwxrwxrwx
  (is (= 511 (setReadable* 511 true true)))
  (is (= 511 (setReadable* 511 true false)))
  (is (= 255 (setReadable* 511 false true)))
  (is (= 219 (setReadable* 511 false false)))
  ; ----r--r--
  (is (=  292 (setReadable* 36 true true)))
  (is (=  292 (setReadable* 36 true false)))
  (is (=  36  (setReadable* 36 false true)))
  (is (=  0   (setReadable* 36 false false))))

(deftest test-setWritable*
  ; rwxrwxrwx
  (is (= 511 (setWritable* 511 true true)))
  (is (= 511 (setWritable* 511 true false)))
  (is (= 383 (setWritable* 511 false true)))
  (is (= 365 (setWritable* 511 false false)))
  ; -----w--w-
  (is (=  146 (setWritable* 18 true true)))
  (is (=  146 (setWritable* 18 true false)))
  (is (=  18  (setWritable* 18 false true)))
  (is (=  0   (setWritable* 18 false false))) 
  ; -----w----
  (is (=  144 (setWritable* 16 true true)))
  (is (=  146 (setWritable* 16 true false)))
  (is (=  16  (setWritable* 16 false true)))
  (is (=  0   (setWritable* 16 false false))))