(defproject cljs-node-io "0.2.0"
  :description "A ClojureScript IO Library for NodeJS"
  :url "https://github.com/pkpkpk/cljs-node-io"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [andare "0.1.0"]]

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-figwheel "0.5.4-7" :exclusions [org.clojure/core.async]]]

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
                                    :main cljs-node-io.test.runner
                                    :parallel-build true
                                    :externs ["node_externs.js"]
                                    :output-to "target/simple.js"
                                    :source-map "target/simple.js.map"                                    
                                    :static-fns true
                                    :optimize-constants true
                                    :language-in :ecmascript5
                                    ;http://dev.clojure.org/jira/browse/CLJS-1627
                                    :closure-warnings
                                      {:check-types :error
                                       :undefined-names :off
                                       :externs-validation :off
                                       :missing-properties :off}}}]

              :test-commands
              ;  "simple" ["node" "target/test/simple.js"]
              {"dev" ["node" "target/out/cljs_node_io.js"]}})
