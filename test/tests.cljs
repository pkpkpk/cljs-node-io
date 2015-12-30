(ns ^:figwheel-always cljs-node-io.tests
  (:require [cljs.test :refer-macros [deftest is testing run-tests are]]
            [cljs-node-io.core :refer [File Coercions as-file as-url file
                                       as-relative-path]]) ;file
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

(deftest test-as-relative-path
  (testing "strings"
    (is (= "foo" (as-relative-path "foo"))))
  (testing "absolute path strings are forbidden"
    (is (thrown? js/Error (as-relative-path (.getAbsolutePath (File. "baz"))))))
  (testing "relative File paths"
    (is (= "bar" (as-relative-path (File. "bar")))))
  (testing "absolute File paths are forbidden"
    (is (thrown? js/Error (as-relative-path (File. (.getAbsolutePath (File. "quux"))))))))




; (deftest test-resources-with-spaces
;   (let [file-with-spaces (temp-file "test resource 2" "txt")
;         url (as-url (.getParentFile file-with-spaces))
;         loader (java.net.URLClassLoader. (into-array [url]))
;         r (resource (.getName file-with-spaces) loader)]
;     (is (= r (as-url file-with-spaces)))
;     (spit r "foobar")
;     (is (= "foobar" (slurp r)))))


;




(run-tests)
