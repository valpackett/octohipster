(ns octohipster.params.json
  (:require [cheshire.core :as json]))

(def json-params
  "JSON params support"
  ^{:ctype-re #"^application/(vnd.+)?json"}
  (fn [body]
    (json/parse-string body true)))
