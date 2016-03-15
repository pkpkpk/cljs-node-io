(ns cljs-node-io.test.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [cljs.test :refer-macros [deftest is testing async are]]
            [cljs.core.async :as async :refer [put! take! chan <! pipe  alts!]]
            [cljs-node-io.file :refer [File createTempFile]]
            [cljs-node-io.protocols :refer [Coercions as-file as-url ]]
            [cljs-node-io.core :refer [file as-relative-path spit slurp aspit aslurp delete-file make-parents]])
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
  (let [f             (createTempFile "spit_slurp_unicode" "test")
        ascii-content (js/Buffer. #js[0x62 0x75 0x66 0x66 0x65 0x72])
        uni-content   (apply str (concat "a" (repeat 500 "\u226a\ud83d\ude03")))]
    (is (= false (.exists f)))
    ;ascii + bin
    (spit f ascii-content :append false )
    (is (= ascii-content (slurp f :encoding ""))) ;raw buffer should be identical
    (is (= "buffer" (slurp f :encoding "ascii"))) ;but content is valid ascii
    ;unicode
    (doseq [enc [ "utf8" "utf-8" "utf16le" "utf-16le" "ucs2" "ucs-2"]]
      (spit f uni-content :encoding enc :append false)
      (is (= uni-content (slurp f :encoding enc))))
    (is (= true (.delete f)))))

(deftest test-delete-file
  (let [f (createTempFile "test" "deletion")
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

(deftest test-async-spit-and-slurp
  (let [txt "hello world"
        f    (createTempFile "test" "deletion")]
    (async done
      (go
        (is (= true (<! (aspit f txt))))
        (is (= txt  (<! (aslurp f))))
        (done)))))
