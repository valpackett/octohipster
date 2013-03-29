(ns octohipster.handlers.yaml
  (:require [clj-yaml.core :as yaml])
  (:use [octohipster.handlers util]))

(def wrap-handler-yaml
  "Wraps a handler with a YAML handler."
  ^{:ctypes ["application/yaml" "application/x-yaml" "text/yaml" "text/x-yaml"]}
  (fn [handler]
    (fn [ctx]
      (case (-> ctx :representation :media-type)
        ("application/yaml" "application/x-yaml"
         "text/yaml" "text/x-yaml")
          (let [result (handler ctx)
                k (:data-key result)]
            (resp-with-links ctx (-> result k yaml/generate-string)))
        (handler ctx)))))
