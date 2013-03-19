(ns swaggerator.handlers.edn
  (:use [swaggerator.handlers util]))

(defn wrap-handler-edn
  "Wraps a handler with a EDN handler."
  [handler]
  (swap! *handled-content-types* conj "application/edn")
  (fn [ctx]
    (case (-> ctx :representation :media-type)
      "application/edn" (let [result (handler ctx)
                              k (:data-key result)]
                           (resp-with-links ctx (-> result k pr-str)))
      (handler ctx))))
