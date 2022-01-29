(defproject homework "0.1.0-SNAPSHOT"
  :description "A simple application for ETL and retrieval of data files"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.apache.logging.log4j/log4j-api "2.17.1"]
                 [org.apache.logging.log4j/log4j-core "2.17.1"]
                 [org.clojure/tools.cli "1.0.206"]
                 ]
  :main ^:skip-aot homework.core
  :jvm-opts ["-Dclojure.tools.logging.factory=clojure.tools.logging.impl/log4j2-factory"]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
