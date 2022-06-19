(ns cljs-node-io.file-tests
  (:require [cljs.test :refer-macros [deftest is testing run-tests are]]
            [clojure.string :as s :refer [starts-with?]]
            [cljs-node-io.file :refer [File setReadable* setWritable* setExecutable*]]
            [cljs-node-io.fs :as fs]
            [cljs-node-io.core :refer [spit slurp] :as io]))

(def os (js/require "os"))
(def path (js/require "path"))
(def process (js/require "process"))

(defn createTempFile
  ([prefix] (createTempFile prefix nil nil))
  ([prefix suffix] (createTempFile prefix suffix nil))
  ([prefix suffix dir]
   (let [tmpd (or dir (.tmpdir os))]
     (File. (str tmpd (.-sep path) prefix (or suffix ".tmp"))))))

; test delete & empty v full directories

(deftest test-createNewFile-and-delete
  (let [f (createTempFile "foo" ".txt")]
    (are [x y] (= x y)
      false (.exists f)
      0     (.length f)
      true  (.createNewFile f)
      true  (zero? (.length f))
      true  (.exists f)
      nil   (.list f)
      false (.createNewFile f)
      true  (.delete f)
      false (.exists f)
      false (.delete f))))


(deftest test-directory-functions
  (let [d  (createTempFile "T_DIR" "")
        d2 (createTempFile "subdir" "" d)
        d3 (createTempFile "subdir2" "" d2)
        foo  (createTempFile "foo" nil d)
        bar  (createTempFile "bar" nil d)
        deep  (createTempFile "deep" nil d3)]
    (.delete d)
    ; create & delete empty directory
    (are [x y] (= x y)
      false (.exists d)
      true  (.mkdir d)
      true  (.exists d)
      true  (.isDirectory d)
      true  (empty? (.list d))
      true  (.delete d)
      false (.exists d)
      false (.isDirectory d)
      true (.mkdir d)
      true (.createNewFile foo)
      true (.createNewFile bar)
      true (.mkdir d2)
      ["bar.tmp" "foo.tmp" "subdir"] (.list d)
      ["foo.tmp"] (.list d (fn [d name] (starts-with? name "foo")))
      ["bar.tmp" "subdir"] (.list d (fn [d name] (not (starts-with? name "foo"))))

      ["/tmp/T_DIR/bar.tmp" "/tmp/T_DIR/foo.tmp" "/tmp/T_DIR/subdir"]
      (mapv #(.getPath %)(.listFiles d))

      (mapv #(File. %) ["/tmp/T_DIR/bar.tmp" "/tmp/T_DIR/subdir"])
      (.listFiles d (fn [d f] (not (starts-with? (.getName f) "foo"))))

      false (.delete d)
      true (.exists foo))
    (is (every? true? (mapv #(.delete %) (list foo bar d2 d))))
    ;derive parent dirs from subdir via mkdir
    (is (every? false? (mapv #(.exists %) (list deep d3 d2 d))))
    (is true (.mkdirs deep)) ; Should make b just like java, which is why core/make-parents exists
    (is (every? true? (mapv #(and (.isDirectory %) (.exists %)) (list deep d3 d2 d))))
    (is (every? true? (mapv #(.delete %) (list deep d3 d2 d))))))

(deftest test-setReadable*
  (are [x y] (= x y)
    ; rwxrwxrwx
    511 (setReadable* 511 true true)
    511 (setReadable* 511 true false)
    255 (setReadable* 511 false true)
    219 (setReadable* 511 false false)
    ; ----r--r--
    292 (setReadable* 36 true true)
    292 (setReadable* 36 true false)
    36  (setReadable* 36 false true)
    0   (setReadable* 36 false false)))

(deftest test-setWritable*
  (are [x y] (= x y)
    ; rwxrwxrwx
    511 (setWritable* 511 true true)
    511 (setWritable* 511 true false)
    383 (setWritable* 511 false true)
    365 (setWritable* 511 false false)
    ; -----w--w-
    146 (setWritable* 18 true true)
    146 (setWritable* 18 true false)
     18 (setWritable* 18 false true)
      0 (setWritable* 18 false false)
    ; -----w----
    144 (setWritable* 16 true true)
    146 (setWritable* 16 true false)
     16 (setWritable* 16 false true)
      0 (setWritable* 16 false false)))

(deftest test-setExecutable*
  (are [x y] (= x y)
    ; rwxrwxrwx
    511 (setExecutable* 511 true true)
    511 (setExecutable* 511 true false)
    447 (setExecutable* 511 false true)
    438 (setExecutable* 511 false false)
    ; ------x--x
    73 (setExecutable* 9 true true)
    73 (setExecutable* 9 true false)
     9 (setExecutable* 9 false true)
     0 (setExecutable* 9 false false)))

(deftest rename-test
  (let [foo (createTempFile "foo")
        _(spit foo "buen dia")
        orig-path (.getPath foo)
        bar (createTempFile "bar")]
    (is (true? (fs/fexists? orig-path)))
    (is (= "buen dia" (slurp foo)))
    (is (not= (.getPath foo) (.getPath bar)))
    (is (true? (.renameTo foo bar)))
    (is (thrown? js/Error (slurp orig-path)))
    (is (= (.getPath foo) (.getPath bar)))
    (is (false? (fs/fexists? orig-path)))
    (is (= "buen dia" (slurp bar)))))

(defn tmp-path [& args]
  (apply path.join (into-array (cons (.tmpdir os) args))))

(defn abs-path [& args]
  (apply path.join (into-array (conj args "tmp" (.cwd process)))))

(deftest absolute-paths-test
  (let [a (createTempFile "a")]
    (is (= (.getAbsolutePath a) "/tmp/a.tmp")))
  (testing "user provided absolute paths"
    (let [child-path (tmp-path "grandparent" "parent" "child")
          child (File. child-path)
          parent (.getParentFile child)]
      (is (true? (.isAbsolute child)))
      (is (true? (.isAbsolute parent)))
      (is (= (.getAbsolutePath parent) (tmp-path "grandparent" "parent")))))
  (testing "derived absolute paths"
    (let [local-tmp (fs/mkdir "tmp" {:recursive true})
          child (File. (path.join "tmp" "grandparent" "parent" "child"))
          parent (.getParentFile child)]
      (is (false? (.isAbsolute child)))
      (is (false? (.isAbsolute parent)))
      (is (= (abs-path "grandparent" "parent" "child") (.getAbsolutePath child)))
      (is (= (abs-path "grandparent" "parent") (.getAbsolutePath parent)))
      (fs/rm-f local-tmp))))
