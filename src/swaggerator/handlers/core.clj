(ns swaggerator.handlers.core
  (:use [swaggerator.link util]
        [swaggerator util]))

(defn wrap-handler-request-links [handler]
  (fn [ctx]
    (-> ctx
        (update-in [:links] concatv (-> ctx :request :links))
        (update-in [:link-templates] concatv (-> ctx :request :link-templates))
        handler)))

(defn wrap-handler-context-relative-links [handler]
  (fn [ctx]
    (let [uri-context (or (-> ctx :request :context) "")
          prepender (partial prepend-to-href uri-context)]
      (-> ctx
          (assoc :links (map prepender (:links ctx)))
          (assoc :link-templates (map prepender (:link-templates ctx)))
          handler))))

(defn wrap-default-handler
  "Wraps a handler with default data transformers"
  [handler]
  (-> handler
      wrap-handler-context-relative-links
      wrap-handler-request-links))

(defn collection-handler
  "Makes a handler that maps a presenter over data that is retrieved
  from the Liberator context by given data key (by default :data)."
  ([presenter] (collection-handler presenter :data))
  ([presenter k]
   (fn [ctx]
     (-> ctx
         (assoc :data-key k)
         (assoc k (mapv presenter (k ctx)))))))

(defn item-handler
  "Makes a handler that applies a presenter to data that is retrieved
  from the Liberator context by given data key (by default :data)."
  ([presenter] (item-handler presenter :data))
  ([presenter k]
   (fn [ctx]
     (-> ctx
         (assoc :data-key k)
         (assoc k (presenter (k ctx)))))))
