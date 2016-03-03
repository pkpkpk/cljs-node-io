(defproject cljs-node-io "0.1.0"
  :description "A ClojureScript IO Library for NodeJS"
  :url "http://example.com/FIXME"


  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [org.clojure/core.async "0.2.374"]]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.5.0-3"]]


  :clean-targets ["server.js"
                  "target"]

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src" "test"]
                        :figwheel true
                        :compiler {
                                   :parallel-build true
                                   :main cljs-node-io.core
                                   :output-to "target/server_dev/cljs_node_io.js"
                                   :output-dir "target/server_dev"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true}}

                        {:id "simple"
                         :source-paths ["src" "test"]
                         :notify-command ["node" "target/test/tests-simple.js"]
                         :compiler {:optimizations :simple
                                    :target :nodejs
                                    :output-to "target/test/tests-simple.js"
                                    :output-dir "target/test/out-simple"}}

                        {:id "advanced"
                         :source-paths ["src" "test"]
                         :compiler {:optimizations :advanced
                                    :target :nodejs
                                    :parallel-build true
                                    :output-to "target/test/tests-advanced.js"
                                    :output-dir "target/test/out-advanced"}}]

              :test-commands
              {"simple" ["node" "target/test/tests-simple.js"]
               "advanced" ["node" "target/test/tests-advanced.js"]}})
