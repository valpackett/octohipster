(ns swaggerator.handlers.core
  (:use [swaggerator.handlers json edn yaml hal cj]
        [swaggerator.link util]
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
  "Wraps a handler with all the data format wrappers"
  [handler]
  (-> handler
      wrap-handler-edn
      wrap-handler-yaml
      wrap-handler-hal-json
      wrap-handler-collection-json
      wrap-handler-json
      wrap-handler-context-relative-links
      wrap-handler-request-links))

(defn list-handler
  "Makes a handler that maps a presenter over data that is retrieved
  from the Liberator context by given data key (by default :data)."
  ([presenter] (list-handler presenter :data))
  ([presenter k]
   (fn [ctx]
     (-> ctx
         (assoc :data-key k)
         (assoc k (mapv presenter (k ctx)))))))

(defn default-list-handler
  "list-handler wrapped in wrap-default-handler."
  ([presenter] (default-list-handler presenter :data))
  ([presenter k] (-> (list-handler presenter k)
                     wrap-default-handler)))

(defn entry-handler
  "Makes a handler that applies a presenter to data that is retrieved
  from the Liberator context by given data key (by default :data)."
  ([presenter] (entry-handler presenter :data))
  ([presenter k]
   (fn [ctx]
     (-> ctx
         (assoc :data-key k)
         (assoc k (presenter (k ctx)))))))

(defn default-entry-handler
  "entry-handler wrapped in wrap-default-handler."
  ([presenter] (default-entry-handler presenter :data))
  ([presenter k] (-> (entry-handler presenter k)
                     wrap-default-handler)))
