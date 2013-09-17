(defproject conclujon "0.1.1"
  :description "A framework for writing and executing active specifications in HTML."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :url "https://github.com/nilswloka/conclujon"
  :scm {:name "git"
        :url "https://github.com/nilswloka/conclujon"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [enlive "1.1.4"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]]
                   :resource-paths ["test_resources"]
                   :source-paths ["dev"]}})
