(ns octohipster.handlers.hal
  (:use [octohipster.handlers util]
        [octohipster.link util]
        [octohipster json util]))

(defn- add-self-link [ctx rel x]
  (assoc x :_links {:self {:href (self-link ctx rel x)}}))

(defn- add-nest-link [ctx rel x y]
  (let [tpl (uri-template-for-rel ctx rel)
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

(def wrap-handler-hal-json
  "Wraps handler with a HAL+JSON handler. Note: consumes links;
  requires wrapping the Ring handler with octohipster.handlers/wrap-hal-json."
  ^{:ctypes ["application/hal+json"]}
  (fn [handler]
    (fn [ctx]
      (case (-> ctx :representation :media-type)
        "application/hal+json"
          (let [rsp (handler ctx)
                dk (:data-key rsp)
                result (dk rsp)
                links (-> rsp response-links-and-templates links-as-map)
                ik (if-let [from-ctx (-> ctx :resource :item-key)]
                     (from-ctx)
                     :item)
                result (cond
                         (map? result) (embedify ctx result)
                         (nil? result) {:_embedded (:_embedded rsp)}
                         :else {:_embedded {dk (map (partial embedify ctx)
                                                    (map (partial add-self-link ctx (name ik))
                                                         result))}})]
            {:body (jsonify (assoc result :_links links))})
        (handler ctx)))))
