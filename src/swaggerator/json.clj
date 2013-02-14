(ns swaggerator.json
  (:require [cheshire.core :as json])
  (:use [swaggerator util]))

; thanks: https://github.com/mmcgrana/ring-json-params/blob/master/src/ring/middleware/json_params.clj

(defn- json-request?
  [req]
  (if-let [#^String type (:content-type req)]
    (not (empty? (re-find #"^application/(vnd.+)?json" type)))))
 
(defn wrap-json-params
  "Ring middleware that parses JSON, updates :params and
  :non-query-params with received data."
  [handler]
  (fn [req]
    (if-let [body (and (json-request? req) (:body req))]
      (let [bstr (slurp body)
            json-params (json/parse-string bstr true)
            req* (assoc req
                   :non-query-params (merge (or (:non-query-params req) {}) json-params)
                   :params (merge (:params req) json-params))]
        (handler req*))
      (handler req))))

(defn jsonify [x] (json/generate-string x))

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
               (assoc :_links (assoc (or (:_links x) {}) :self {:href (full-uri req)}))
               jsonify)}))
