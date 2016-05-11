(ns cljs-node-io.test.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [cljs.test :refer-macros [deftest is testing async are]]
            [cljs.core.async :as async :refer [put! take! chan <! pipe  alts!]]
            [cljs-node-io.file :refer [File createTempFile]]
            [cljs-node-io.streams :refer [BufferReadStream BufferWriteStream]]
            [cljs-node-io.protocols :refer [Coercions as-file as-url ]]
            [cljs-node-io.core :as core
             :refer [file rFile?  Buffer? copy as-relative-path
                     spit slurp aspit aslurp
                     reader writer delete-file make-parents]])
  (:import goog.Uri))

(def fs (js/require "fs"))
(def stream (js/require "stream"))

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

(deftest test-async-spit-and-slurp
  (let [f             (createTempFile "aspit_aslurp" "test")
        ascii-content (js/Buffer. #js[0x62 0x75 0x66 0x66 0x65 0x72])
        uni-content   (apply str (concat "a" (repeat 500 "\u226a\ud83d\ude03")))]
    (async done
      (go
        (.delete f)
        (is (= false (.exists f)))
        (is (= true (<! (aspit f ascii-content :append false))))
        (is (= ascii-content (<! (aslurp f :encoding ""))))
        (is (= "buffer" (<! (aslurp f :encoding "ascii"))))
        ;unicode
        (doseq [enc [ "utf8" "utf-8" "utf16le" "utf-16le" "ucs2" "ucs-2"]]
          (= true (<! (aspit f uni-content :encoding enc :append false)))
          (is (= uni-content (<! (aslurp f :encoding enc)))))
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

(def os (js/require "os"))

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

(defn bytes-should-equal [buffer-1 buffer-2]
  (is (and (Buffer? buffer-1) (Buffer? buffer-2)))
  (is (= (into []  (array-seq buffer-1)) (into [] (array-seq buffer-2)))))

(defn data-fixture
  "in memory fixture data for tests"
  [src-enc]
  (let [s  (apply str (concat "a" (repeat 10 "\u226a\ud83d\ude03")))
        i  (js/Buffer. s src-enc)
        ch (chan)
        o  (BufferWriteStream (fn [b] (put! ch b)) )]
    {:s s :input-buffer i :o o :ch ch}))

; buffers convert strings to different encodings via the toString method
; for new Buffer(str, enc), enc identifies the str params encoding, ex:
;   buf = new Buffer('7468697320697320612074c3a97374', 'hex')
;   buf.toString("utf8") ;=> this is a tést

(deftest test-copy-encodings
  (async done
   (go
    (doseq [enc [ "utf8" "utf-8" "utf16le" "utf-16le" "ucs2" "ucs-2"]]
      (testing (str "from inputstream " enc " to output UTF-8")
        (let [{:keys [s input-buffer o ch]} (data-fixture enc)]
          (is (nil? (copy input-buffer o :encoding "utf8")))
          (let [expected (js/Buffer. (.toString input-buffer))
                output-buffer (<! ch)]
            (bytes-should-equal expected output-buffer))))
      (testing (str "from inputstream UTF-8 to output-stream  " enc)
        (let [{:keys [o s ch input-buffer]} (data-fixture "utf8")]
          (is (nil? (copy input-buffer o :encoding enc)))
          (let [expected (js/Buffer. (.toString (js/Buffer. s) enc))
                output-buffer (<! ch)]
            (bytes-should-equal expected output-buffer )))))
    (done))))


(deftest test-IOFactory
  (let [r-w (juxt reader writer)
        f (createTempFile "foo" "bar")
        _ (spit f "")
        p (.getPath f)
        strs  (r-w p)
        [ifs ofs :as files] (r-w f)]
    ;files + strings
    (is (= [fs.ReadStream fs.WriteStream] (mapv type strs)))
    (is (= [fs.ReadStream fs.WriteStream] (mapv type files)))
    (is (and (= ifs (reader ifs)) (= ofs (writer ofs)))) ;streams returns themselves
    (is (thrown? js/Error (writer ifs))) ;filein
    (is (thrown? js/Error (reader ofs))) ;fileoutstream
    ;Buffer + BufferStreams
    (let [b   (js/Buffer #js [1 2 3])
          rbs (reader b)
          wbs (BufferWriteStream nil  nil)]
      (is (thrown? js/Error  (writer b)))
      (is (= [stream.Readable stream.Writable] (mapv type [rbs wbs])))
      (is (= rbs (reader rbs)))
      (is (= wbs (writer wbs)))
      (is (thrown? js/Error  (writer rbs)))
      (is (thrown? js/Error  (reader wbs))))      
    (run! #(.destroy %) (flatten [strs files]))    
    (.delete f)))