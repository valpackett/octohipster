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
  (route "/:name" [name]
    (resource "Operations with individual things"
      :method-allowed? (request-method-in :get :put :head)
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
      :handle-ok (fn [ctx] (str "Name: " name)))))

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
                                       :errorResponses [{:code 422
                                                         :reason "The data did not pass schema validation"}
                                                        {:code 404
                                                         :reason "Resource not found"}]
                                       :parameters [{:name "name"
                                                     :description "Name"
                                                     :dataType "string"
                                                     :required true
                                                     :paramType "path"}]}]}]
                 :models {:Thing {:id "Thing"
                                  :properties {:name {:type "string"}}
                                  :required ["name"]}}})))

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
      (should= 201 (:status x)))))
