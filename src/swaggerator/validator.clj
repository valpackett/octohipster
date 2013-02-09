(ns swaggerator.validator
  (:import [com.github.fge.jsonschema.main JsonSchema JsonSchemaFactory]
           [com.github.fge.jsonschema.util JsonLoader]
           [com.fasterxml.jackson.core JsonFactory]
           [com.fasterxml.jackson.databind JsonNode ObjectMapper]
           [java.io StringWriter])
  (:require [cheshire.core :as json]
            [cheshire.factory :as factory]))

(def mapper (ObjectMapper.))

(defn ^JsonNode clojure->jsonnode [x]
  (JsonLoader/fromString (json/generate-string x))) ; any better way of doing this?

(defn ^JsonSchema make-schema-object [schema]
  (.fromSchema (JsonSchemaFactory/defaultFactory)
               (clojure->jsonnode schema)))

(defn make-validator [schema]
  (let [so (make-schema-object schema)]
    (fn [x]
      (let [root (.asJsonObject (.validate so (clojure->jsonnode x)))
            sw (StringWriter.)
            jgen (.createJsonGenerator ^JsonFactory (or factory/*json-factory* factory/json-factory) sw)]
        (.writeTree mapper jgen root)
        (.toString sw)))))

(defn wrap-json-schema-validator [handler schema]
  (let [v (make-validator schema)]
    (fn [req]
      (if (#{:post :put :patch} (-> req :request-method))
        (let [results (-> req :json-params v)]
          (if (= results "{}")
            (handler req)
            {:status 422
             :headers {"Content-Type" "application/json;charset=utf-8"}
             :body results}))
        (handler req)))))
