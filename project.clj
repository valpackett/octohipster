(defproject org.clojars.myfreeweb.swaggerator/swaggerator "0.1.2-SNAPSHOT"
  :description "A REST API framework with Swagger support"
  :url "https://github.com/myfreeweb/swaggerator"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.reader "0.7.0"]
                 [ring/ring-core "1.1.8"]
                 [compojure "1.1.5"]
                 [liberator "0.8.0"]
                 [cheshire "5.0.2"]
                 [clj-yaml "0.4.0"]
                 [clj-msgpack "0.2.0"]
                 [inflections "0.8.0"]
                 [com.github.fge/json-schema-validator "1.99.9"]
                 [com.damnhandy/handy-uri-templates "1.1.7"]]
  :profiles {:1.5 {:dependencies [[org.clojure/clojure "1.5.0"]]}
             :dev {:dependencies [[speclj "2.5.0"                 :exclusions [org.clojure/clojure]]
                                  [com.novemberain/monger "1.4.2" :exclusions [org.clojure/clojure]]
                                  [http-kit "2.0.0-RC4"           :exclusions [org.clojure/clojure]]
                                  [ring-ratelimit "0.1.0"         :exclusions [org.clojure/clojure]]
                                  [ring-mock "0.1.3"              :exclusions [org.clojure/clojure]]]}}
  :plugins [[speclj "2.5.0"]
            [codox "0.6.4"]
            [lein-release "1.0.0"]]
  :aliases {"all" ["with-profile" "dev:dev,1.5"]}
  :bootclasspath true
  :lein-release {:deploy-via :lein-deploy}
  :repositories [["snapshots" {:url "https://clojars.org/repo" :creds :gpg}]
                 ["releases"  {:url "https://clojars.org/repo" :creds :gpg}]]
  :warn-on-reflection true
  :jar-exclusions [#"example.clj"]
  :codox {:exclude example
          :src-dir-uri "https://github.com/myfreeweb/swaggerator/blob/master"
          :src-linenum-anchor-prefix "L"}
  :test-paths ["spec/"])
