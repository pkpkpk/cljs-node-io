(ns cljs-node-io.lock-file-tests
  (:require [cljs.core.async :refer [<! put! take! chan go promise-chan]]
            [cljs.test :refer [deftest is testing  use-fixtures async]]
            [cljs-node-io.core :as io :refer [spit slurp]]
            [cljs-node-io.fs :as fs]))

(defn err?
  [[?err]]
  (instance? js/Error ?err))

(defn ok?
  [[?err]]
  (nil? ?err))

(defn guarded-async-write
  [pathstr content]
  (go
    (let [[?err lock :as res] (<! (fs/alock-file pathstr))]
      (if ?err
        res
        (let [[?err :as res] (<! (fs/awriteFile pathstr content nil))]
          (if ?err
            res
            (<! (.release lock))))))))

(def test-file-path "/tmp/lock-test-file")
(def lock-file-path (str test-file-path ".LOCK"))

(deftest AsyncLockFile-test
  (io/delete-file test-file-path true)
  (io/delete-file lock-file-path true)
  (async done
   (go
    (is (= [nil] (<! (guarded-async-write test-file-path "buenas"))))
    (is (= "buenas" (slurp test-file-path)))
    (let [[?err lock] (<! (fs/alock-file test-file-path))]
      (is (nil? ?err))
      (is (instance? fs/AsyncLockFile lock))
      (testing "when a lock is already held, a guarded write cannot occur"
        (is (true? (<! (fs/afexists? lock-file-path))))
        (is (err? (<! (guarded-async-write test-file-path "should never be written"))))
        (is (= "buenas" (io/slurp test-file-path))))
      (testing "releasing the lock should cleanup the lock-file and allow cycle to repeat"
        (is (ok? (<! (.release lock))))
        (is (false? (<! (fs/afexists? lock-file-path))))
        (is (ok? (<! (guarded-async-write test-file-path "dime lo mami"))))
        (is (false? (<! (fs/afexists? lock-file-path))))
        (is (= "dime lo mami" (io/slurp test-file-path)))))
    (done))))

(defn guarded-write
  [pathstr content]
  (let [lock (fs/lock-file pathstr)]
    (fs/writeFile pathstr content nil)
    (.release lock)))

(deftest LockFile-test
  (io/delete-file test-file-path true)
  (io/delete-file lock-file-path true)
  (guarded-write test-file-path "que lo que")
  (is (= "que lo que" (io/slurp test-file-path)))
  (let [lock (fs/lock-file test-file-path)]
    (is (instance? fs/LockFile lock))
    (testing "when a lock is already held, a guarded write cannot occur"
      (is (true? (fs/fexists? lock-file-path)))
      (is (thrown? js/Error (guarded-write test-file-path "should never be written")))
      (is (= "que lo que" (io/slurp test-file-path))))
    (testing "closing the file should cleanup locks and allow cycle to repeat"
      (.release lock)
      (is (false? (fs/fexists? lock-file-path)))
      (guarded-write test-file-path "dime lo mami")
      (is (false? (fs/fexists? lock-file-path)))
      (is (= "dime lo mami" (io/slurp test-file-path))))))
