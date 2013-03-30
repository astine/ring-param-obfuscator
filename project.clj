(defproject ring-param-obfuscator "0.1.0-SNAPSHOT"
  :description "Ring middleware for obfuscating parameter lists"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [ring/ring-core "1.1.8"]
                 [ring/ring-codec "1.0.0"]
                 [org.clojure/tools.reader "0.7.3"]]
  :profiles {:dev {:dependencies [[ring-serve "0.1.2"]]}})
