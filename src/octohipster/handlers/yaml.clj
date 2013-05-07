(ns octohipster.handlers.yaml
  (:require [clj-yaml.core :as yaml])
  (:use [octohipster.handlers util]))

(defhandler wrap-handler-yaml
  "Wraps a handler with a YAML handler."
  ["application/yaml" "application/x-yaml" "text/yaml" "text/x-yaml"]
  (make-handler-fn yaml/generate-string))
