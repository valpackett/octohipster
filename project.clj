(defproject octohipster "0.2.1-SNAPSHOT"
  :description "A hypermedia REST HTTP API library for Clojure"
  :url "https://github.com/myfreeweb/octohipster"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-core "1.2.0-beta2"]
                 [ring.middleware.jsonp "0.1.2"]
                 [liberator "0.8.0"]
                 [clout "1.1.0"]
                 [cheshire "5.1.1"]
                 [clj-yaml "0.4.0"]
                 [inflections "0.8.0"]
                 [org.bovinegenius/exploding-fish "0.3.3"]
                 [com.github.fge/json-schema-validator "2.1.3"]
                 [com.damnhandy/handy-uri-templates "1.1.7"]]
  :profiles {:dev {:dependencies [[speclj "2.6.1"                 :exclusions [org.clojure/clojure]]
                                  [com.novemberain/monger "1.4.2" :exclusions [org.clojure/clojure]]
                                  [http-kit "2.0.1"               :exclusions [org.clojure/clojure]]
                                  [ring-mock "0.1.3"              :exclusions [org.clojure/clojure]]]}}
  :plugins [[speclj "2.6.1"]
            [codox "0.6.4"]
            [lein-release "1.0.0"]]
  :bootclasspath true
  :lein-release {:deploy-via :lein-deploy}
  :repositories [["snapshots" {:url "https://clojars.org/repo" :creds :gpg}]
                 ["releases"  {:url "https://clojars.org/repo" :creds :gpg}]]
  :warn-on-reflection true
  :jar-exclusions [#"example.clj"]
  :codox {:exclude example
          :src-dir-uri "https://github.com/myfreeweb/octohipster/blob/master"
          :src-linenum-anchor-prefix "L"}
  :test-paths ["spec/"])
