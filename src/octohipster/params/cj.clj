(ns octohipster.params.cj
  (:require [cheshire.core :as json]))

(defn- into-kv [{:keys [name value]}]
  [(keyword name) value])

(def collection-json-params
  "Collection+JSON params support"
  ^{:ctype-re #"^application/vnd\.collection\+json"}
  (fn [body]
    (->> (json/parse-string body true)
         :template :data
         (map into-kv)
         (into {}))))
