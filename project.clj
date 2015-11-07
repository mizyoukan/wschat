(defproject wschat "0.1.0-SNAPSHOT"
  :description "A chat application using WebSocket for training"
  :url "https://github.com/mizyoukan/wschat"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.2.371"]
                 [http-kit "2.1.19"]
                 [compojure "1.4.0"]
                 [org.clojure/clojurescript "1.7.145"]
                 [reagent "0.5.1"]
                 [re-frame "0.5.0"]
                 [jarohen/chord "0.6.0"]]

  :source-paths ["src/clj"]

  :main ^{:skip-aot true} wschat.server

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.4.1" :exclusions [cider/cider-nrepl]]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs"]

                        :figwheel {:on-jsload "wschat.core/mount-root"}

                        :compiler {:main wschat.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :asset-path "js/compiled/out"
                                   :source-map-timestamp true}}

                       {:id "min"
                        :source-paths ["src/cljs"]
                        :compiler {:main wschat.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :optimizations :advanced
                                   :pretty-print false}}]}

  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]]}
             :uberjar {:main wschat.server
                       :aot :all
                       :prep-tasks ["compile" ["cljsbuild" "once" "min"]]}}

  :repl-options {:init-ns wschat.server
                 :init (start-server)})
