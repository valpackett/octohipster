(ns swaggerator.handlers.edn
  (:use [swaggerator.handlers util]))

(def wrap-handler-edn
  "Wraps a handler with a EDN handler."
  ^{:ctypes ["application/edn"]}
  (fn [handler]
    (fn [ctx]
      (case (-> ctx :representation :media-type)
        "application/edn" (let [result (handler ctx)
                                k (:data-key result)]
                             (resp-with-links ctx (-> result k pr-str)))
        (handler ctx)))))
