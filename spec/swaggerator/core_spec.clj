(ns swaggerator.core-spec
  (:require [cheshire.core :as json])
  (:use [speclj core]
        [ring.mock request]
        [swaggerator core]))

(def thing-schema
  {:id "Thing"
   :properties {:name {:type "string"}}
   :required [:name]})

(defcontroller things "/things"
  "Operations about things"
  (route "/:name" [name]
    (entry-resource "Operations with individual things"
      :schema thing-schema
      :doc {:get {:nickname "getThing"
                  :responseClass "Thing"
                  :summary "Get the thing"
                  :notes "Notes"
                  :parameters [{:name "name"
                                :description "Name"
                                :dataType "string"
                                :required true
                                :paramType "path"}]}}
      :data-key :data
      :exists? (fn [ctx] {:data 1})
      :presenter (fn [data] {:data data}))))

(defroutes app-routes
  things)

(describe "swaggerator"
  (it "nests controllers"
    (let [x (-> (request :get "/things/something")
                (header "Accept" "application/json")
                app-routes :body)]
      (should= x "{\"data\":1}")))

  (it "outputs the resource listing"
    (let [x (-> (request :get "/api-docs.json") app-routes :body (json/parse-string true))]
      (should= x {:swaggerVersion "1.1"
                  :basePath "http://localhost"
                  :apis [{:path "/api-docs.json/things"
                          :description "Operations about things"}]})))

  (it "outputs api declarations"
    (let [x (-> (request :get "/api-docs.json/things") app-routes :body (json/parse-string true))]
      (should= (:models x)
               {:Thing {:id "Thing"
                        :properties {:name {:type "string"}}
                        :required ["name"]}})
      (should= (:apis x)
               [{:path "/things/{name}"
                 :description "Operations with individual things"
                 :operations [{:httpMethod "GET"
                               :nickname "getThing"
                               :summary "Get the thing"
                               :notes "Notes"
                               :responseClass "Thing"
                               :errorResponses [{:code 422
                                                 :reason "The data did not pass schema validation"}
                                                {:code 404
                                                 :reason "Resource not found"}]
                               :parameters [{:name "name"
                                             :description "Name"
                                             :dataType "string"
                                             :required true
                                             :paramType "path"}]}]}])
      (should= (:resourcePath x) "/things")))

  (it "uses json schema for validation"
    (let [x (-> (request :put "/things/something")
                (content-type "application/json")
                (body (json/generate-string {:name 1}))
                app-routes)]
      (should= 422 (:status x))
      (should= "integer" (-> x :body json/parse-string (get "/name") first (get "found"))))
    (let [x (-> (request :put "/things/something")
                (content-type "application/json")
                (body (json/generate-string {:name "str"}))
                app-routes)]
      (should= 200 (:status x))))

  (it "outputs the raw json schema"
    (let [x (-> (request :get "/things.schema") app-routes)
          b (-> x :body (json/parse-string true))]
      (should= (-> x :headers (get "Content-Type")) "application/schema+json;charset=UTF-8")
      (should= (:id b) "Thing")))

  (it "links to the raw json schema"
    (let [x (-> (request :get "/things/something")
                (header "Accept" "application/json")
                app-routes)]
      (should= (-> x :headers (get "Link")) "</things/something>; rel=\"self\", </things.schema#>; rel=\"describedBy\"")))

  (it "outputs the schema for hal"
    (let [x (-> (request :get "/all.schema")
                (content-type "application/hal+json")
                app-routes :body (json/parse-string true))]
      (should= (keys x) [:_links :Thing])))

  (it "outputs the root for hal"
    (let [x (-> (request :get "/")
                (content-type "application/hal+json")
                app-routes :body (json/parse-string true))]
      (should= (:_links x) {:self {:href "/"}
                            :things {:href "/things" :title "Operations about things"}})
      (should= (-> x :_embedded :schema keys) [:_links :Thing]))))
