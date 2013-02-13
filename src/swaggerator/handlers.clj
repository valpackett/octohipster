(ns swaggerator.handlers
  (:use [swaggerator json link]))

(def ^:dynamic *handled-content-types* (atom []))

(defn wrap-handler-json
  ([handler] (wrap-handler-json handler :data))
  ([handler k]
    (swap! *handled-content-types* conj "application/json")
    (fn [ctx]
      (case (-> ctx :representation :media-type)
        "application/json" (-> ctx handler k jsonify)
        (handler ctx)))))

(defn wrap-handler-link [handler]
  (fn [ctx]
    (let [result (handler ctx)]
      (if (map? result)
        (assoc result :links (:links ctx))
        {:body result
         :links (:links ctx)}))))

(defn wrap-default-handler [handler]
  (-> handler
      wrap-handler-json
      wrap-handler-link))

(defn list-handler
  ([presenter] (list-handler presenter :data))
  ([presenter k]
   (fn [ctx]
     (assoc ctx k (mapv presenter (k ctx))))))

(defn default-list-handler
  ([presenter] (default-list-handler presenter :data))
  ([presenter k] (-> (list-handler presenter k)
                     wrap-default-handler)))

(defn entry-handler
  ([presenter] (entry-handler presenter :data))
  ([presenter k]
   (fn [ctx]
     (assoc ctx k (presenter (k ctx)))))) 

(defn default-entry-handler
  ([presenter] (default-entry-handler presenter :data))
  ([presenter k] (-> (entry-handler presenter k)
                     wrap-default-handler)))
