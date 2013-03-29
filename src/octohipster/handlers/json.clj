(ns octohipster.handlers.json
  (:use [octohipster.handlers util]
        [octohipster json]))

(def wrap-handler-json
  "Wraps a handler with a JSON handler."
  ^{:ctypes ["application/json"]}
  (fn [handler]
    (fn [ctx]
      (case (-> ctx :representation :media-type)
        "application/json" (let [result (handler ctx)
                                 k (:data-key result)]
                             (resp-with-links ctx (-> result k jsonify)))
        (handler ctx)))))
