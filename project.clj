(defproject cljs-node-io "0.1.0"
  :description "A ClojureScript IO Library for NodeJS"
  :url "http://example.com/FIXME"


  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [org.clojure/clojurescript "1.9.227"]
                 [org.clojure/test.check "0.9.0" :scope "test"]
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

                        {:id "simple"
                         :source-paths ["src" "test"]
                         :notify-command ["node" "target/test/simple.js"]
                         :compiler {:optimizations :simple
                                    :target :nodejs
                                    :parallel-build true
                                    :main cljs-node-io.test.runner
                                    :externs ["node_externs.js"]
                                    :output-to "target/test/simple.js"
                                    :output-dir "target/test/"
                                    :source-map "target/test/simple.js.map"
                                    :language-in :ecmascript5
                                    ; :static-fns true ;=> err
                                    :optimize-constants true
                                     :closure-warnings
                                      {:check-types :error ; CLJS-1627
                                       :undefined-names :off
                                       :externs-validation :off
                                       :missing-properties :off}}}]

              :test-commands
              {"simple" ["node" "target/test/simple.js"]}})
