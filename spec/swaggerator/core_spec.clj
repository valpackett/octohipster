(ns swaggerator.core-spec
  (:require [cheshire.core :as json])
  (:use [speclj core]
        [ring.mock request]
        [compojure.core :only [defroutes]]
        [swaggerator core]))

(def thing-schema
  {:id "Thing"
   :properties {:name {:type "string"}}
   :required [:name]})

(defcontroller things "/things"
  "Operations about things"
  (resource "/:name" [name] "Operations with individual things"
    :method-allowed? [:get :head]
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
    :handle-ok (fn [ctx] (str "Name: " name))))

(defroutes app-routes
  (nest things)
  (swagger-routes things))

(describe "swaggerator"
  (it "nests controllers"
    (let [x (-> (request :get "/things/something") app-routes :body)]
      (should= x "Name: something")))

  (it "outputs the resource listing"
    (let [x (-> (request :get "/api-docs.json") app-routes :body (json/parse-string true))]
      (should= x {:swaggerVersion "1.1"
                  :basePath "http://localhost"
                  :apis [{:path "/api-docs.json/things"
                          :description "Operations about things"}]})))

  (it "outputs api declarations"
    (let [x (-> (request :get "/api-docs.json/things") app-routes :body (json/parse-string true))]
     (should= x {:swaggerVersion "1.1"
                 :basePath "http://localhost"
                 :resourcePath "/things"
                 :description "Operations about things"
                 :apis [{:path "/things/{name}"
                         :description "Operations with individual things"
                         :operations [{:httpMethod "GET"
                                       :nickname "getThing"
                                       :summary "Get the thing"
                                       :notes "Notes"
                                       :responseClass "Thing"
                                       :errorResponses []
                                       :parameters [{:name "name"
                                                     :description "Name"
                                                     :dataType "string"
                                                     :required true
                                                     :paramType "path"}]}]}]
                 :models {:Thing {:id "Thing"
                                  :properties {:name {:type "string"}}
                                  :required ["name"]}}}))))
