(ns swaggerator.util
  (:require [clojure.string :as string])
  (:import [com.damnhandy.uri.template UriTemplate]
           [java.net URLEncoder]))

(defn concatv [& xs] (into [] (apply concat xs)))

(defn clout->uri-template [x]
  (string/replace x #":([^/]+)" "{$1}"))

(defmacro map-to-querystring
  "Turns a map into a query sting, eg.
  {:abc 123 :def ' '} -> ?abc=123&def=+"
  [m]
  `(if (empty? ~m) ""
     (->> ~m
          (map #(str (-> % key name (URLEncoder/encode "UTF-8"))
                     "="
                     (-> % val str  (URLEncoder/encode "UTF-8"))))
          (interpose "&")
          (apply str "?"))))

(defn alter-query-params [req x]
  (map-to-querystring (merge (:query-params req) x)))

(defn uri-alter-query-params [req x]
  (str (:uri req) (alter-query-params req x)))

(defn uri-template-for-rel [ctx rel]
  (-> (filter #(= (:rel %) rel) (or ((-> ctx :resource :link-templates)) []))
      first
      :href))

(defn expand-uri-template [tpl x]
  (let [tpl (UriTemplate/fromTemplate tpl)]
    (doseq [[k v] x]
      (.set tpl (name k) v))
    (.expand tpl)))

(defn full-uri [req]
  (str (:uri req)
       (if-let [qs (:query-string req)]
         (str "?" qs)
         "")))

(defn wrap-handle-options-and-head [handler]
  (fn [req]
    (case (:request-method req)
      (:head :options) (-> req
                           (assoc :request-method :get)
                           handler
                           (assoc :body nil))
      (handler req))))

(defn wrap-cors [handler]
  (fn [req]
    (let [rsp (handler req)]
      (assoc rsp :headers (merge (-> rsp :headers)
                                 {"Access-Control-Allow-Origin" "*"
                                  "Access-Control-Allow-Headers" "Accept, Authorization, Content-Type"
                                  "Access-Control-Allow-Methods" "GET, POST, DELETE, PUT"})))))
