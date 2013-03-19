(ns swaggerator.handlers.json
  (:use [swaggerator.handlers util]
        [swaggerator json]))

(defn wrap-handler-json
  "Wraps a handler with a JSON handler."
  [handler]
  (swap! *handled-content-types* conj "application/json")
  (fn [ctx]
    (case (-> ctx :representation :media-type)
      "application/json" (let [result (handler ctx)
                               k (:data-key result)]
                           (resp-with-links ctx (-> result k jsonify)))
      (handler ctx))))
