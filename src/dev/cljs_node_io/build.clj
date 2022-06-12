(ns cljs-node-io.build
  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]
            [clojure.pprint :refer [pprint]]
            [cljs.build.api :as api]
            [cljs.util]))

; https://github.com/seancorfield/build-clj

(def lib 'com.github.pkpkpk/cljs-node-io)

(def version (format "2.0.%s" (b/git-count-revs nil)))
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn clean
  ([] (b/delete {:path "target"}))
  ([_] (b/delete {:path "target"})))

(defn jar
  [opts]
  (-> opts
    (assoc :class-dir nil
           :src-pom "./template/pom.xml"
           :lib lib
           :version version
           :basis basis
           :jar-file jar-file
           :src-dirs ["src/main"])
    bb/jar))

(defn ci "Run the CI pipeline of tests (and build the JAR)." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/run-tests)
      (bb/clean)
      (bb/jar)))

(defn install "Install the JAR locally." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/install)))

(defn deploy "Deploy the JAR to Clojars." [opts]
  (-> opts
    (assoc :lib lib :version version)
    (bb/deploy)))

(def simple
  {:target :nodejs
   :optimizations :simple
   :verbose true
   :externs ["node_externs.js"]
   :main 'cljs-node-io.runner
   :parallel-build true
   :static-fns true
   :optimize-constants true
   :language-in :ecmascript-2015
   :output-to "simple.js"
   :closure-warnings
   {:check-types :warning
    :undefined-names :off
    :externs-validation :off
    :missing-properties :off}})

(def advanced
  {:externs ["externs/events.js"
             "externs/stream.js"
             "externs/buffer.js"
             "externs/fs.js"
             "externs/path.js"
             "externs/os.js"
             "externs/process.js"
             "externs/net.js"
             "externs/child_process.js"],
   :static-fns true,
   ; :pseudo-names true
   :optimize-constants true,
   :optimizations :advanced,
   :parallel-build true,
   ; :verbose true,
   :output-to "out/advanced.js",
   :target :nodejs,
   :main 'cljs-node-io.runner,
   :language-in :ecmascript-2015})
