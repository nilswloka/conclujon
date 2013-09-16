(defproject conclujon "0.1.0-SNAPSHOT"
  :description "A framework for writing and executing active specifications in HTML."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [enlive "1.1.4"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]]
                   :resource-paths ["test_resources"]
                   :source-paths ["dev"]}})
