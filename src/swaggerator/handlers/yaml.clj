(ns swaggerator.handlers.yaml
  (:require [clj-yaml.core :as yaml])
  (:use [swaggerator.handlers util]))

(defn wrap-handler-yaml
  "Wraps a handler with a YAML handler."
  [handler]
  (swap! *handled-content-types* conj "application/yaml")
  (swap! *handled-content-types* conj "application/x-yaml")
  (swap! *handled-content-types* conj "text/yaml")
  (swap! *handled-content-types* conj "text/x-yaml")
  (fn [ctx]
    (case (-> ctx :representation :media-type)
      ("application/yaml" "application/x-yaml"
       "text/yaml" "text/x-yaml")
        (let [result (handler ctx)
              k (:data-key result)]
          (resp-with-links ctx (-> result k yaml/generate-string)))
      (handler ctx))))
