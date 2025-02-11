(defproject matthiasn/meins "0.0-SNAPSHOT"
  :description "meins - a personal information manager"
  :url "https://github.com/matthiasn/meins"
  :license {:name "GNU AFFERO GENERAL PUBLIC LICENSE"
            :url  "https://www.gnu.org/licenses/agpl-3.0.en.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.logging "0.5.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [hiccup "1.0.5"]
                 [org.clojure/data.avl "0.1.0"]
                 [org.clojure/test.check "0.10.0-alpha3"]
                 [clj-pid "0.1.2"]
                 [clj-time "0.15.1"]
                 [clj-http "3.10.0"]
                 [ring/ring-core "1.7.1"]
                 [enlive "1.1.6"]
                 [buddy/buddy-sign "3.1.0"]
                 [me.raynes/fs "1.4.6"]
                 [markdown-clj "1.10.0"]
                 [progrock "0.1.2"]
                 [cheshire "5.8.1"]
                 [me.raynes/conch "0.8.0"]
                 [com.climate/claypoole "1.1.4"]
                 [org.clojure/data.csv "0.1.4"]

                 [com.walmartlabs/lacinia "0.32.0"]
                 [com.walmartlabs/lacinia-pedestal "0.12.0"]

                 [org.eclipse.jetty/jetty-server "9.4.7.v20170914"]
                 [org.eclipse.jetty.websocket/websocket-api "9.4.7.v20170914"]
                 [org.eclipse.jetty.websocket/websocket-server "9.4.7.v20170914"]

                 [vincit/venia "0.2.5"]
                 [metrics-clojure "2.10.0"]
                 [metrics-clojure-jvm "2.10.0"]
                 [com.taoensso/nippy "2.14.0" :exclusions [com.taoensso/encore]]
                 [com.taoensso/timbre "4.10.0" :exclusions [io.aviso/pretty]]
                 [com.drewnoakes/metadata-extractor "2.12.0"]
                 [ubergraph "0.6.1"]
                 [camel-snake-kebab "0.4.0"]
                 [matthiasn/systems-toolbox "0.6.40"]
                 [matthiasn/systems-toolbox-sente "0.6.32"]
                 [org.clojure/tools.reader "1.3.2"]
                 [clucy "0.4.0"]
                 [clj.qrgen "0.4.0"]
                 [image-resizer "0.1.10"]
                 [danlentz/clj-uuid "0.1.9"]]

  :source-paths ["src/cljc" "src/clj/"]

  :clean-targets ^{:protect false} ["prod/main"
                                    "prod/renderer"
                                    "prod/geocoder"
                                    "prod/updater"
                                    "target"
                                    "out"
                                    "dev/renderer"]
  :auto-clean false
  :uberjar-name "meins.jar"

  :main meins.jvm.core
  :jvm-opts ["-Xmx2g"]

  :profiles {:uberjar {:aot :all}
             :dev     {:source-paths ["src/cljc" "src/clj/" "dev-resources" "dev"]
                       :dependencies [;[io.dgraph/dgraph4j "1.7.1"]
                                      [factual/geo "2.1.1"]
                                      [org.clojure/tools.namespace "0.3.0"]]}}

  :repl-options {:init-ns meins.jvm.core}

  :doo {:paths {:karma "./node_modules/karma/bin/karma"}}

  :plugins [[test2junit "1.4.2"]
            [lein-cloverage "1.1.1"]
            [deraen/lein-sass4clj "0.3.1"]
            [lein-shell "0.5.0"]
            [lein-jlink "0.2.1"]
            [lein-ancient "0.6.15"]]

  :jlink-modules ["java.base" "java.sql" "java.desktop" "java.naming"
                  "java.management" "jdk.unsupported" "jdk.crypto.cryptoki"]

  ;:global-vars {*assert* false}

  :test2junit-run-ant true

  :sass {:source-paths ["src/scss/"]
         :target-path  "resources/public/css/"}

  :aliases {"sass" ["sass4clj" "once"]
            "dist" ["do"
                    ["clean"]
                    ["test"]
                    ["cljs-main"]
                    ["cljs-renderer"]
                    ["sass4clj" "once"]
                    ["jlink" "assemble"]]})
