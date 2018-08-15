(defproject rpg-action "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [compojure "1.6.1"]
                 [ring "1.7.0-RC1"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-json "0.4.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [cheshire "5.8.0"]
                 [metosin/spec-tools "0.7.1"]
                 [orchestra "2017.11.12-1"]]
  :plugins [[lein-ring "0.12.4"]]
  :ring {:handler rpg-action.handler/app}
  :profiles {:dev  {:dependencies [[javax.servlet/servlet-api "2.5"]
                                   [ring/ring-mock "0.3.2"]]
                    :injections [(require 'rpg-action.handler)
                                 (require 'orchestra.spec.test)
                                 (orchestra.spec.test/instrument)]}
             :test {:injections [(require 'rpg-action.handler)
                                 (require 'orchestra.spec.test)
                                 (orchestra.spec.test/instrument)]}})
