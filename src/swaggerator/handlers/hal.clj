(ns swaggerator.handlers.hal
  (:use [swaggerator.handlers util]
        [swaggerator.link util]
        [swaggerator json util]))

(defn- add-self-link [ctx dk x]
  (assoc x :_links {:self {:href (self-link ctx dk x)}}))

(defn- add-nest-link [ctx rel x y]
  (let [lm (or ((-> ctx :resource :embed-mapping)) {})
        tpl (uri-template-for-rel ctx rel)
        href (expand-uri-template tpl (merge x y))]
    (-> y
        (assoc :_links {:self {:href href}}))))

(defn- embedify [ctx x]
  (if-let [mapping (-> ctx :resource :embed-mapping)]
    (let [mapping (mapping)]
      (-> x
          (select-keys (filter #(not (mapping %)) (keys x)))
          (assoc :_embedded
            (into {}
              (map (fn [[k rel]] [k (mapv #(add-nest-link ctx rel x %) (x k))])
                   mapping)))))
    x))

(defn wrap-handler-hal-json
  "Wraps handler with a HAL+JSON handler. Note: consumes links;
  requires wrapping the Ring handler with swaggerator.handlers/wrap-hal-json."
  [handler]
  (swap! *handled-content-types* conj "application/hal+json")
  (fn [ctx]
    (case (-> ctx :representation :media-type)
      "application/hal+json"
        (let [rsp (handler ctx)
              dk (:data-key rsp)
              result (dk rsp)
              links (-> rsp response-links-and-templates links-as-map)
              result (if (map? result)
                       (embedify ctx result)
                       {:_embedded {dk (map (partial embedify ctx) (map (partial add-self-link ctx dk) result))}})]
          {:body (jsonify (assoc result :_links links))})
      (handler ctx))))
