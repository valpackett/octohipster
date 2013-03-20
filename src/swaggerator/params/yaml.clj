(ns swaggerator.params.yaml
  (:require [clj-yaml.core :as yaml]))

(def yaml-params
  "YAML params support"
  ^{:ctype-re #"^(application|text)/(vnd.+)?(x-)?yaml"}
  (fn [body]
    (yaml/parse-string body)))
