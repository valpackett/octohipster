(ns octohipster.validator
  (:import [com.github.fge.jsonschema.main JsonValidator JsonSchemaFactory]
           [com.github.fge.jsonschema.report ProcessingReport]
           [com.github.fge.jackson JsonLoader]
           [com.fasterxml.jackson.core JsonFactory]
           [com.fasterxml.jackson.databind JsonNode ObjectMapper]
           [java.io StringWriter])
  (:require [cheshire.core :as json]
            [cheshire.factory :as factory]))

(def mapper (ObjectMapper.))

(defn ^JsonNode clojure->jsonnode [x]
  (JsonLoader/fromString (json/generate-string x))) ; any better way of doing this?

(defn ^JsonValidator make-validator-object []
  (.getValidator (JsonSchemaFactory/byDefault)))

(defn make-validator [schema]
  (let [v (make-validator-object)]
    (fn [x]
      (.validate v (clojure->jsonnode schema) (clojure->jsonnode x)))))

(defn is-success? [^ProcessingReport report]
  (.isSuccess report))

(defn to-json [^ProcessingReport report]
  (let [sw (StringWriter.)
        jgen (.createJsonGenerator (or factory/*json-factory* factory/json-factory) sw)]
    (.writeTree ^ObjectMapper mapper jgen (.asJson report))
    (.toString sw)))

(defn wrap-json-schema-validator
  "Ring middleware that validates any POST/PUT requests
  (:non-query-params) against a given JSON Schema."
  [handler schema]
  (let [v (make-validator schema)]
    (fn [req]
      (if (#{:post :put :patch} (-> req :request-method))
        (let [result (-> req :non-query-params v)]
          (if (is-success? result)
            (handler req)
            {:status 422
             :headers {"Content-Type" "application/json;charset=utf-8"}
             :body (to-json result)}))
        (handler req)))))
