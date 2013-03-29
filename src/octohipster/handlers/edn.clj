(ns octohipster.handlers.edn
  (:use [octohipster.handlers util]))

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
