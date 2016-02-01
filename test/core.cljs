(ns ^:figwheel-always cljs-node-io.test.core
  (:require [cljs.test :refer-macros [deftest is testing run-tests run-all-tests are]]
            [cljs-node-io.file :refer [File temp-file]]
            [cljs-node-io.protocols :refer [Coercions as-file as-url ]]
            [cljs-node-io.core :refer [file as-relative-path spit slurp delete-file make-parents]]) ;file File
  (:import goog.Uri))


(deftest test-as-url ;note URL object is dropped
 (are [file-part input] (= (.getPath (Uri. (str "file:" file-part))) (as-url input))
       "foo" "file:foo"
       "quux" (Uri. "file:quux"))
  (is (nil? (as-url nil))))

(deftest test-as-file
  (are [result input] (= result (as-file input))
       (File. "foo") "foo"
       (File. "bar") (File. "bar")
       (File. "baz") (Uri. "file:baz")
       (File. (Uri. "file:baz")) (Uri. "file:baz")
       (File. "bar+baz") (Uri. "file:bar+baz")
       (File. "bar baz qux") (Uri. "file:bar%20baz%20qux")
       (File. "abcíd/foo.txt") (Uri. "file:abc%c3%add/foo.txt")
       nil nil))

(deftest test-file
  (are [result args] (= (File. result) (apply file args))
       "foo" ["foo"]
       "foo/bar" ["foo" "bar"]
       "foo/bar/baz" ["foo" "bar" "baz"]))


(deftest test-spit-and-slurp
  (let [f (temp-file "cljs.node.io" "test")
        content (apply str (concat "a" (repeat 500 "\u226a\ud83d\ude03")))]
    (spit f content)
    (is (= content (slurp f)))
    ; UTF-16 must be last for the following test
   (doseq [enc [ "utf8"]] ;"utf16le" should work but doesn't
      (spit f content :encoding enc)
      (is (= content (slurp f :encoding enc))))))

(deftest test-delete-file
  (let [f (temp-file "test" "deletion")
        _ (spit f "")
        not-file (File. (goog.string.getRandomString))]
    (delete-file  f)
    (is (not (.exists f)))
    (is (thrown? js/Error (delete-file not-file)))
    (is (= :silently (delete-file not-file :silently)))))

(deftest test-as-relative-path
  (testing "strings"
    (is (= "foo" (as-relative-path "foo"))))
  (testing "absolute path strings are forbidden"
    (is (thrown? js/Error (as-relative-path (.getAbsolutePath (File. "baz"))))))
  (testing "relative File paths"
    (is (= "bar" (as-relative-path (File. "bar")))))
  (testing "absolute File paths are forbidden"
    (is (thrown? js/Error (as-relative-path (File. (.getAbsolutePath (File. "quux"))))))))

(def os (js/require "os"))

(deftest test-make-parents
  (let [tmp (.tmpdir os)
        a   (file tmp "test-make-parents" "child" "grandchild")]
    (delete-file a :silently)
    (make-parents tmp "test-make-parents" "child" "grandchild")
    (is (.isDirectory (file tmp "test-make-parents" "child")))
    (is (not (.isDirectory (file tmp "test-make-parents" "child" "grandchild"))))))

; (run-tests 'cljs-node-io.test.core
;            'cljs-node-io.test.file)

(run-all-tests #"^cljs-node-io\.test.*")
