(ns octohipster.validator-spec
  (:use [speclj core]
        [ring.mock request]
        [octohipster.params core json]
        [octohipster.handlers json edn]
        [octohipster json validator]))

(defn handler [req] {:status 200})
(def schema
  {:id "Contact"
   :type "object"
   :properties {:name {:type "string"}}
   :required [:name]})
(def app (-> handler
             (wrap-json-schema-validator schema [wrap-handler-edn wrap-handler-json])
             (wrap-params-formats [json-params])))

(describe "wrap-json-schema-validator"
  (it "validates POST and PUT requests"
    (should= 200
             (-> (request :post "/")
                 (content-type "application/json")
                 (body (jsonify {:name "aaa"}))
                 app :status))
    (should= 200
             (-> (request :put "/")
                 (content-type "application/json")
                 (body (jsonify {:name "aaa"}))
                 app :status))
    (should= 422
             (-> (request :put "/")
                 (content-type "application/json")
                 (body (jsonify {:name 1234}))
                 app :status))
    (should= 422
             (-> (request :post "/")
                 (content-type "application/json")
                 (body (jsonify {:name 1234}))
                 app :status)))
  (it "uses content negotiation"
    (should= "error"
             (-> (request :post "/")
                 (header "Accept" "application/edn")
                 (content-type "application/json")
                 (body (jsonify {:name 1234}))
                 app :body read-string first :level))))
