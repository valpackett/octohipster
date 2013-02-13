(defproject swaggerator "0.1.0-SNAPSHOT"
  :description "A REST API framework with Swagger support"
  :url "https://github.com/myfreeweb/swaggerator"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [ring/ring-core "1.1.8"]
                 [compojure "1.1.5"]
                 [liberator "0.8.0"]
                 [cheshire "5.0.1"]
                 [com.github.fge/json-schema-validator "1.99.3"]]
  :profiles {:dev {:dependencies [[speclj "2.5.0"]
                                  [com.novemberain/monger "1.4.2"]
                                  [http-kit "2.0.0-RC4"]
                                  [ring-ratelimit "0.1.0"]
                                  [ring-mock "0.1.3"]]}}
  :plugins [[speclj "2.5.0"]
            [lein-release "1.0.0"]]
  :bootclasspath true
  :lein-release {:deploy-via :lein-deploy}
  :repositories [["snapshots" {:url "https://clojars.org/repo" :creds :gpg}]
                 ["releases"  {:url "https://clojars.org/repo" :creds :gpg}]]
  :warn-on-reflection true
  :jar-exclusions [#"example.clj"]
  :test-paths ["spec/"])
