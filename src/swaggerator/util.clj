(ns swaggerator.util
  (:require [clojure.string :as string])
  (:import [com.damnhandy.uri.template UriTemplate]))

(defn concatv [& xs] (into [] (apply concat xs)))

(defn swaggerify-url-template [x]
  (string/replace x #":([^/]+)" "{$1}"))

(defmacro map-to-querystring
  "Turns a map into a query sting, eg.
  {:abc 123 :def ' '} -> ?abc=123&def=+"
  [m]
  `(if (empty? ~m) ""
     (->> ~m
          (map #(str (java.net.URLEncoder/encode (-> % key name) "UTF-8")
                     "="
                     (java.net.URLEncoder/encode (-> % val str) "UTF-8")))
          (interpose "&")
          (apply str "?"))))

(defn alter-query-params [req x]
  (map-to-querystring (merge (:query-params req) x)))

(defn uri-alter-query-params [req x]
  (str (:uri req) (alter-query-params req x)))

(defn uri-template-for-rel [ctx rel]
  (UriTemplate/fromTemplate
    (-> (filter #(= (:rel %) rel) (or ((-> ctx :resource :link-templates)) []))
        first
        :href)))

(defn set-to-uri-template! [tpl x]
  (doseq [[k v] x]
    (.set tpl (name k) v)))
