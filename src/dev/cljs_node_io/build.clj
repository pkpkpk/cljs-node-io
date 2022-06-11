(ns cljs-node-io.build
  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]))

; https://github.com/seancorfield/build-clj

(def lib 'pkpkpk/cljs-node-io)

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
