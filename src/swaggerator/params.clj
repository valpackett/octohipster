(ns swaggerator.params
  (:require [cheshire.core :as json]
            [clj-yaml.core :as yaml]
            [clj-msgpack.core :as mp]
            [clojure.tools.reader.edn :as edn])
  (:import [org.msgpack MessagePack])
  (:use [swaggerator util]))

; thanks: https://github.com/mmcgrana/ring-json-params/blob/master/src/ring/middleware/json_params.clj

(defn- edn-request? [req]
  (if-let [#^String type (:content-type req)]
    (not (empty? (re-find #"^application/(vnd.+)?edn" type)))))

(defn wrap-edn-params
  "Ring middleware that parses EDN, updates :params and
  :non-query-params with received data."
  [handler]
  (fn [req]
    (if-let [body (and (edn-request? req) (:body req))]
      (let [edn-params (-> body slurp edn/read-string)
            req* (assoc req
                   :non-query-params (merge (or (:non-query-params req) {}) edn-params)
                   :params (merge (:params req) edn-params))]
        (handler req*))
      (handler req))))

(defn- yaml-request? [req]
  (if-let [#^String type (:content-type req)]
    (not (empty? (re-find #"^(application|text)/(vnd.+)?(x-)?yaml" type)))))

(defn wrap-yaml-params
  "Ring middleware that parses YAML, updates :params and
  :non-query-params with received data."
  [handler]
  (fn [req]
    (if-let [body (and (yaml-request? req) (:body req))]
      (let [yaml-params (-> body slurp yaml/parse-string)
            req* (assoc req
                   :non-query-params (merge (or (:non-query-params req) {}) yaml-params)
                   :params (merge (:params req) yaml-params))]
        (handler req*))
      (handler req))))

(defn- msgpack-request? [req]
  (if-let [#^String type (:content-type req)]
    (not (empty? (re-find #"^application/(vnd.+)?(x-)?msgpack" type)))))

(defn wrap-msgpack-params
  "Ring middleware that parses MessagePack, updates :params and
  :non-query-params with received data."
  [handler]
  (fn [req]
    (if-let [body (and (msgpack-request? req) (:body req))]
      (let [msgpack-params (first (map mp/unwrap (.createUnpacker (MessagePack.) body)))
            req* (assoc req
                   :non-query-params (merge (or (:non-query-params req) {}) msgpack-params)
                   :params (merge (:params req) msgpack-params))]
        (handler req*))
      (handler req))))

(defn- json-request? [req]
  (if-let [#^String type (:content-type req)]
    (not (empty? (re-find #"^application/(vnd.+)?json" type)))))

(defn wrap-json-params
  "Ring middleware that parses JSON, updates :params and
  :non-query-params with received data."
  [handler]
  (fn [req]
    (if-let [body (and (json-request? req) (:body req))]
      (let [json-params (-> body slurp (json/parse-string true))
            req* (assoc req
                   :non-query-params (merge (or (:non-query-params req) {}) json-params)
                   :params (merge (:params req) json-params))]
        (handler req*))
      (handler req))))
