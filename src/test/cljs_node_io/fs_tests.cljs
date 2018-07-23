(ns cljs-node-io.fs-tests
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer [deftest is testing run-tests are use-fixtures async]]
            [cljs.core.async :refer [<! put! take! chan] :as async]
            [cljs-node-io.fs :as iofs]))

(def os (js/require "os"))
(def path (js/require "path"))
(def fs (js/require "fs"))

(def tmp ^{:doc "@type {string}"} (.tmpdir os))

(def dtree
  [{:type :dir :p (path.join tmp "D")}
   {:type :fff :p (path.join tmp "D" "f0.txt") :ext ".txt"}
   {:type :dir :p (path.join tmp "D" "d0")}
   {:type :dir :p (path.join tmp "D" "d1")}
   {:type :fff :p (path.join tmp "D" "d0" "d0f0.txt") :ext ".txt"}
   {:type :fff :p (path.join tmp "D" "d1" "d1f0.ggg") :ext ".ggg"}
   {:type :dir :p (path.join tmp "D" "d0" "dd0")}
   {:type :dir :p (path.join tmp "D" "d0" "dd0" "ddd0")}
   {:type :fff :p (path.join tmp "D" "d0" "dd0" "ddd0" "ffff0.foo.bar") :ext ".bar"}])

(def file-paths (into [] (comp (remove #(= (:type %) :dir)) (map :p)) dtree))
(def file-exts  (into [] (comp (remove #(= (:type %) :dir)) (map :ext)) dtree))
(def dirs       (into [] (comp (filter #(= (:type %) :dir)) (map :p)) dtree))
(def root (:p (first dtree)))
(def all-paths  (into [] (concat file-paths (reverse dirs))))
(def others #{42 #js[] #js{} #() (js/Buffer. #js[]) nil "" js/NaN})


(defn teardown []
  (let [f? (fn [p](try (.isFile (fs.statSync p)) (catch js/Error e false)))
        d? (fn [p](try (.isDirectory (fs.statSync p)) (catch js/Error e false)))
        rd (fn [dpath] (-> (fs.readdirSync dpath) array-seq))
        rm (fn rm [p]
             (if (d? p)
               (do
                 (doseq [i (map (partial path.resolve p) (rd p))]
                   (rm i))
                 (try (fs.rmdirSync p) (catch js/Errror e nil)))
               (try (fs.unlinkSync p) (catch js/Error e nil))))]
    (rm root)))

(defn setup []
  (teardown)
  (doseq [{:keys [type p]} dtree]
    (if (= type :dir)
      (fs.mkdirSync p)
      (fs.writeFileSync p "utf8" ""))))

(defn err-as-val
  "returns error object instead of throwing"
  [f] (fn [item]
     (try (f item)
       (catch js/Error e e))))

(defn all-errors? [f coll]
  (assert (fn? f) "the 1st argument to all-errors must be a function")
  (assert (coll? coll) "the 2nd argument to all-errors must be a collection")
  (every? #(instance? js/Error %) (map (err-as-val f) coll)))

(defn ^boolean Error? [obj] (instance? js/Error obj))

(defn ecode [[e]] (.-code e))

(deftest bad-types
  (doseq [o (into #{} (remove #(js/Buffer.isBuffer %)) (disj others ""))]
    (is (thrown? js/Error (iofs/armdir o)))
    (is (thrown? js/Error (iofs/arm-r o)))
    (is (thrown? js/Error (iofs/arm o)))
    (is (thrown? js/Error (iofs/aunlink o)))
    (is (thrown? js/Error (iofs/arename "" o)))
    (is (thrown? js/Error (iofs/arename o ""))))
  (doseq [o (disj others "")]
    ;path
    (is (thrown? js/Error (iofs/absolute? o)))
    (is (thrown? js/Error (iofs/dirname o)))
    (is (thrown? js/Error (iofs/basename o)))
    (is (thrown? js/Error (iofs/ext o)))
    (is (thrown? js/Error (iofs/resolve-path o)))
    ;sync
    (is (thrown? js/Error (iofs/dir? o)))
    (is (thrown? js/Error (iofs/file? o)))
    (is (thrown? js/Error (iofs/fexists? o)))
    (is (thrown? js/Error (iofs/readdir o)))
    (is (thrown? js/Error (iofs/unlink o)))
    (is (thrown? js/Error (iofs/rmdir o)))
    (is (thrown? js/Error (iofs/rm o)))
    (is (thrown? js/Error (iofs/rm-r o)))
    (is (thrown? js/Error (iofs/rename "foo" o)))
    (is (thrown? js/Error (iofs/rename o "foo")))
    (is (thrown? js/Error (iofs/writeFile o "foo" nil)))
    ;async
    (is (thrown? js/Error (iofs/adir? o)))
    (is (thrown? js/Error (iofs/afile? o)))
    (is (thrown? js/Error (iofs/afexists? o)))
    (is (thrown? js/Error (iofs/areaddir o)))))


(deftest sync-fs-reads-and-path
  (testing "file?, dir?, fexists? absolute?"
    (is (every? iofs/absolute? all-paths))
    (is (not-any? iofs/absolute? (map iofs/basename all-paths)))
    (is (every? iofs/file? file-paths))
    (is (not-any? iofs/file? dirs))
    (is (not-any? iofs/dir? file-paths))
    (is (every? iofs/dir? dirs))
    (is (every? iofs/fexists? all-paths)))
  (testing "readdir, dirname, resolve-path, basename, ext"
    (let [parent root
          children (mapv :p (subvec dtree 1 4)) ;key parent-child relationships in dtree to lose this hard indexing
          children-dirnames  (into #{} (map iofs/dirname) children)
          children-basenames (into #{} (map iofs/basename) children)]
     (is (= "." (iofs/dirname "")))
     (is (= #{parent} children-dirnames))
     (is (= children-basenames (set (iofs/readdir parent))))
     (is (= children (mapv iofs/resolve-path (repeat (first children-dirnames)) children-basenames)))))
  (is (thrown? js/Error (iofs/readdir "")) "readdir should IO error ''")
  (is (thrown? js/Error (iofs/readdir (first file-paths))) "readdir should IO error a file"))

(deftest async-reads
  (async done
   (go
    (testing "afile?, adir?, afexists?"
      (let [f (first file-paths)
            d (first dirs)]
        (is (true?  (<! (iofs/afexists? d))))
        (is (true?  (<! (iofs/afexists? f))))
        (is (true?  (<! (iofs/afile? f))))
        (is (false? (<! (iofs/afile? d))))
        (is (true?  (<! (iofs/adir? d))))
        (is (false? (<! (iofs/adir? f))))))
    (testing "areaddir"
      (let [children (mapv :p (subvec dtree 1 4)) ;key parent-child relationships in dtree to lose this hard indexing
            children-basenames (into #{} (map iofs/basename) children)
            [err res] (<! (iofs/areaddir root))]
        (is (= children-basenames (set res)))))
    (testing "readdir IO errors"
      (doseq [o #{"" (first file-paths)}]
        (let [[err res] (<! (iofs/areaddir o))]
          (is (some? err)))))
    (done))))

(deftest sync-writes
  (testing "rename, writeFile"
    (let [a (first file-paths)
          b (path.join (first dirs) "foo")]
      (is (thrown? js/Error (iofs/rename b a )) "trying to rename a non-existing file should throw")
      (is (boolean ((set (iofs/readdir (first dirs))) (iofs/basename a))))
      (is (nil? (iofs/rename a b)) "rename should return nil")
      (is (boolean ((set (iofs/readdir (first dirs))) (iofs/basename b))))
      (is (nil? (iofs/rename b a)) "rename should return nil")))
  (testing "mkdir, unlink, rmdir, rm-r"
    (is (all-errors? iofs/rmdir (reverse dirs)) "rmdir on a non-empty dir should throw")
    (is (all-errors? iofs/rm  (reverse dirs)) "rm on a non-empty dir should throw")
    (is (all-errors? iofs/unlink dirs) "attempting to unlink a dir should throw")
    (is (every? nil? (map iofs/unlink file-paths)) "unlinking a file should return nil")
    (is (every? nil? (map iofs/rmdir (reverse dirs))) "rmdir success should return nil")
    (is (all-errors? iofs/rmdir (reverse dirs)) "trying to rmdir a non-existent dir should throw")
    (is (all-errors? iofs/mkdir (rest dirs)) "mkdir with non-existent parent should throw")
    (is (every? nil? (map iofs/mkdir dirs)) "mkdir should return nil")
    (is (all-errors? iofs/mkdir dirs) "mkdir on an existing dir should throw")
    (is (every? nil? (map (fn [p] (iofs/writeFile p "" nil)) file-paths)) "writeFile should return nil")
    (is (nil? (iofs/rm-r root)) "rm-r returns nil")
    (is (thrown? js/Error (iofs/rm-r root)) "rm-r throws when given non-existing path"))
  (testing "empty string IO errors"
    (is (thrown? js/Error (iofs/unlink "")))
    (is (thrown? js/Error (iofs/rmdir "")))
    (is (thrown? js/Error (iofs/rm "")))
    (is (thrown? js/Error (iofs/rm-r "")))
    (is (thrown? js/Error (iofs/rename (first file-paths) "")))
    (is (thrown? js/Error (iofs/rename "" "foo")))
    (is (thrown? js/Error (iofs/writeFile "" "" nil)))))

(deftest async-writes-1
 (async done
  (go
   (testing "amkdir, armdir, awriteFile, aunlink, arm"
     (is (= "ENOTEMPTY" (ecode (<! (iofs/armdir (last dirs))))) "armdir on a non-empty dir should return [err]")
     (is (= "ENOTEMPTY" (ecode (<! (iofs/arm (last dirs))))) "armdir on a non-empty dir should return [err]")
     (is (= "ENOTDIR" (ecode (<! (iofs/armdir (last file-paths))))) "armdir on a file should return [err]")
     (is (= [nil] (<! (iofs/aunlink (last file-paths)))) "aunlink success should return [nil]")
     (is (= "ENOENT" (ecode (<! (iofs/aunlink (last file-paths))))) "aunlink on an non-existing file should return [err]")
     (is (= "EISDIR" (ecode (<! (iofs/aunlink (last dirs))))) "aunlink on a dir should return [err]")
     (is (= [nil] (<! (iofs/armdir (last dirs)))) "armdir success should return [nil]")
     (is (= "ENOENT" (ecode (<! (iofs/armdir (last dirs))))) "armdir on a non-existing dir should return [err]")
     (is (= [nil] (<! (iofs/arm (second (reverse dirs))))) "arm success on a dir should return [nil]")
     (is (= "ENOENT" (ecode (<! (iofs/arm (second (reverse dirs)))))) "arm on a non-existing dir should return [err]")
     (is (= "ENOENT" (ecode (<! (iofs/amkdir (last dirs)))))  "amkdir with non-existent parent should return [err]")
     (is (= "ENOENT" (ecode (<! (iofs/awriteFile (last file-paths) "" nil)))) "awriteFile with no parent dir should return [err]")
     (is (= [nil] (<! (iofs/amkdir (second (reverse dirs))))) "amkdir success should return [nil]")
     (is (= [nil] (<! (iofs/amkdir (last dirs)))) "amkdir success should return [nil]")
     (is (= "EEXIST" (ecode (<! (iofs/amkdir (last dirs))))) "amkdir on an existing directory should return [err]")
     (is (= [nil] (<! (iofs/awriteFile (last file-paths) "" nil))) "awriteFile success should return [nil]")
     (is (= [nil] (<! (iofs/arm (last file-paths)))) "arm success on a file should return [nil]")
     (is (= "ENOENT" (ecode (<! (iofs/arm (last file-paths))))) "arm on a non-existing file should return [err]"))
   (done))))

(deftest async-writes-2
 (async done
  (go
   (testing "arename"
     (let [d (first dirs)
           a (first file-paths)
           b (path.join d "foo")]
       (is (= "ENOENT" (ecode (<! (iofs/arename b a )))) "trying to rename a non-existing file should throw")
       (is (= "ENOENT" (ecode (<! (iofs/arename a "" )))) "trying to rename to a empty string should throw")
       (is (boolean ((set (iofs/readdir d)) (iofs/basename a))))
       (is (= [nil] (<! (iofs/arename a b))) "arename should return [nil]")
       (is (boolean ((set (iofs/readdir d)) (iofs/basename b))))
       (iofs/rename b a)))
   (testing "watch"
     (testing "watch file"
       (let [f (first file-paths)
             w (iofs/watch f)]
         (<! (iofs/atouch f))
         (is (= [f [:change]] (<! w)))
         (.close w)
         (is (= [f [:close]] (<! w)))))
     (testing "watch dir"
       (let [d (first dirs)
             f (first file-paths)
             _  (<! (iofs/arm f))
             w (iofs/watch d)]
         (<! (iofs/atouch f))
         (is (= [d [:rename]] (<! w)))
         (<! (iofs/atouch f))
         (is (= [d [:change]] (<! w)))
         (.close w)
         (is (= [d [:close]] (<! w))))))
   (testing "arm-r"
     (is (every? true? (map iofs/fexists? all-paths)))
     (is (= "ENOENT"  (ecode (<! (iofs/arm-r "")))))
     (is (= [nil] (<! (iofs/arm-r root))))
     (is (every? false? (map iofs/fexists? all-paths))))
   (done))))

(use-fixtures :each {:before setup :after teardown})
