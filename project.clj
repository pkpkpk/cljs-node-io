(defproject cljs-node-io "0.1.0"
  :description "A ClojureScript IO Library for NodeJS"
  :url "http://example.com/FIXME"


  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [org.clojure/clojurescript "1.9.227"]
                 [andare "0.1.0"]]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.4-7"]]


  :clean-targets ["target"]

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src" "test"]
                        :figwheel true
                        :compiler {:parallel-build true
                                   :cache-analysis true
                                   :main cljs-node-io.test.runner
                                   :output-to "target/out/cljs_node_io.js"
                                   :output-dir "target/out"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true}}

                        ;http://dev.clojure.org/jira/browse/ASYNC-110
                        {:id "simple"
                         :source-paths ["src" "test"]
                         :compiler {:optimizations :simple
                                    :target :nodejs
                                    :parallel-build true
                                    :externs ["node_externs.js"]
                                    :output-to "target/test/simple.js"
                                    :output-dir "target/test/"
                                    :source-map "target/test/simple.js.map"
                                    :language-in :ecmascript5
                                    :static-fns true
                                    :optimize-constants true
                                    ;http://dev.clojure.org/jira/browse/CLJS-1627
                                    :closure-warnings
                                      {:check-types :error
                                       :undefined-names :off
                                       :externs-validation :off
                                       :missing-properties :off}
                                    }}]

              :test-commands
              ;  "simple" ["node" "target/test/simple.js"]
              {"dev" ["node" "target/out/cljs_node_io.js"]}})
