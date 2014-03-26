(defproject inkwell "0.1.0-SNAPSHOT"
  :description "Improved interactive and functional programming for Quil"
  :url "https://github.com/solita/inkwell"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.typed "0.2.40"]
                 [quil "1.7.0"]]
  :plugins [[lein-midje "3.1.1"]]
  :core.typed {:check [inkwell.core]}
  :profiles {:dev {:dependencies [[midje "1.6.3"]]}}
  :aliases {"check" ["do" ["typed" "check"] "midje"]})
