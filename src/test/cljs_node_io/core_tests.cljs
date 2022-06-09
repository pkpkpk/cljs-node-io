(ns cljs-node-io.core-tests
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [cljs.test :refer-macros [deftest is testing async are]]
            [cljs.core.async :as async :refer [put! take! chan <! pipe  alts!]]
            [cljs-node-io.file :refer [File]]
            [cljs-node-io.core :as io
             :refer [file   Buffer? copy as-relative-path as-url as-file
                     spit slurp aspit aslurp filepath
                     reader writer delete-file make-parents]])
  (:import goog.Uri))

(def fs (js/require "fs"))
(def path (js/require "path"))
(def stream (js/require "stream"))
(def os (js/require "os"))

(defn createTempFile
  ([prefix] (createTempFile prefix nil nil))
  ([prefix suffix] (createTempFile prefix suffix nil))
  ([prefix suffix dir]
   (let [tmpd (or dir (.tmpdir os))]
     (File. (str tmpd (.-sep path) prefix (or suffix ".tmp"))))))

(defn readable? [o]
  (instance? stream.Readable o))

(defn writable? [o]
  (instance? stream.Writable o))

(deftest IOFactory-test
  (spit (createTempFile "foo") "")
  (testing "nil"
    (is (thrown? js/Error (io/reader nil)))
    (is (thrown? js/Error (io/writer nil)))
    (is (thrown? js/Error (io/input-stream nil)))
    (is (thrown? js/Error (io/output-stream nil))))
  (testing "string"
    (is (readable? (io/reader "foo")))
    (is (writable? (io/writer "foo")))
    (is (readable? (io/input-stream "foo")))
    (is (writable? (io/output-stream "foo"))))
  (testing "file"
    (is (readable? (io/reader (io/file "foo"))))
    (is (writable? (io/writer (io/file "foo"))))
    (is (readable? (io/input-stream (io/file "foo"))))
    (is (writable? (io/output-stream (io/file "foo")))))
  (testing "stream.Readable"
    (is (readable? (io/reader (new stream.Readable))))
    (is (thrown? js/Error (io/writer (new stream.Readable))))
    (is (readable? (io/input-stream (new stream.Readable))))
    (is (thrown? js/Error (io/output-stream (new stream.Readable)))))
  (testing "stream.Writable"
    (is (thrown? js/Error (io/reader (new stream.Writable))))
    (is (writable? (io/writer (new stream.Writable))))
    (is (thrown? js/Error (io/input-stream (new stream.Writable))))
    (is (writable? (io/output-stream (new stream.Writable)))))
  (testing "stream.Duplex"
    (let [s (new stream.Duplex)]
      (is (and (readable? s) (writable? s)))
      (is (identical? s (io/reader s)))
      (is (identical? s (io/writer s)))
      (is (identical? s (io/input-stream s)))
      (is (identical? s (io/output-stream s)))))
  (testing "fs.ReadStream"
    (let [s (new fs.ReadStream "foo")]
      (is (and (readable? (io/reader s)) (identical? s (io/reader s))))
      (is (thrown? js/Error (io/writer s)))
      (is (readable? (io/input-stream s)))
      (is (thrown? js/Error (io/output-stream s)))
      (.close s)))
  (testing "fs.WriteStream"
    (let [s (new fs.createWriteStream "foo")]
      (is (thrown? js/Error (io/reader s)))
      (is (and (writable? (io/writer s)) (identical? s (io/writer s))))
      (is (thrown? js/Error (io/input-stream s)))
      (is (writable? (io/output-stream s)))
      (.close s)))
  (testing "buffers"
    (let [b (js/Buffer.from #js[0 1 2 3])]
      (is (readable? (io/reader b)))
      (is (thrown? js/Error (io/writer b)))
      (is (readable? (io/input-stream b)))
      (is (thrown? js/Error (io/output-stream b))))))

;;==============================================================================

(deftest test-filepath
  (is (= (filepath "foo") "foo"))
  (is (= (filepath "foo" "bar") (path.join "foo" "bar")))
  (is (= (filepath (Uri. "foo")) "foo"))
  (is (= (filepath (File. "foo") "bar")) (path.join "foo" "bar"))
  (is (thrown? js/Error (filepath (File. "foo") (File. "bar")))))

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
        ascii-content (js/Buffer.from #js[0x62 0x75 0x66 0x66 0x65 0x72])
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

(deftest test-async-spit-and-slurp
  (let [f             (createTempFile "aspit_aslurp" "test")
        ascii-content (js/Buffer.from #js[0x62 0x75 0x66 0x66 0x65 0x72])
        uni-content   (apply str (concat "a" (repeat 500 "\u226a\ud83d\ude03")))]
    (async done
      (go
        (.delete f)
        (is (= false (.exists f)))
        (is (= [nil] (<! (aspit f ascii-content :append false))))
        (is (=  [nil ascii-content] (<! (aslurp f :encoding ""))))
        (is (= [nil "buffer"] (<! (aslurp f :encoding "ascii"))))
        ;unicode
        (doseq [enc [ "utf8" "utf-8" "utf16le" "utf-16le" "ucs2" "ucs-2"]]
          (is (= [nil] (<! (aspit f uni-content :encoding enc :append false))))
          (is (= [nil uni-content] (<! (aslurp f :encoding enc)))))
        (= true (.delete f))
        (done)))))

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

(deftest test-make-parents
  (let [tmp (.tmpdir os)]
    (delete-file (file tmp "test-make-parents" "child" "grandchild") :silently)
    (delete-file (file tmp "test-make-parents" "child") :silently)
    (delete-file (file tmp "test-make-parents") :silently)
    (make-parents tmp "test-make-parents" "child" "grandchild")
    (is (.isDirectory (file tmp "test-make-parents" "child")))
    (is (not (.isDirectory (file tmp "test-make-parents" "child" "grandchild"))))
    (delete-file (file tmp "test-make-parents" "child"))
    (delete-file (file tmp "test-make-parents"))))

(deftest acopy-buffer-test
  (async done
    (go
     (testing "coerce buffer to readable for piping to file "
       (let [output-path "copydst"
             output-file (createTempFile output-path)
             input (js/Buffer.from (into-array (range 0 255)))]
         (is (= [nil] (<! (io/acopy input output-file))))
         (is (= input (slurp output-file :encoding "")))))
     (done))))
