(ns octohipster.json
  (:require [cheshire.core :as json])
  (:use [octohipster util]))

(defn jsonify [x] (json/generate-string x))

(defn unjsonify [x] (json/parse-string x true))

(defn serve-json [x]
  (fn [req]
    {:status 200
     :headers {"Content-Type" "application/json;charset=UTF-8"}
     :body (jsonify x)}))

(defn serve-json-schema [x]
  (fn [req]
    {:status 200
     :headers {"Content-Type" "application/schema+json;charset=UTF-8"}
     :body (jsonify x)}))

(defn serve-hal-json [x]
  (fn [req]
    {:status 200
     :headers {"Content-Type" "application/hal+json;charset=UTF-8"}
     :body (-> x
               (assoc :_links (assoc (or (:_links x) {}) :self {:href (context-relative-uri req)}))
               jsonify)}))
