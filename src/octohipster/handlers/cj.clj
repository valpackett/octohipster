(ns octohipster.handlers.cj
  (:use [octohipster.handlers util]
        [octohipster.link util]
        [octohipster json util]))

(defn- transform-map [[k v]]
  {:name k, :value (if (map? v) (mapv transform-map v) v)})

(defn- cj-wrap [ctx rel m]
  {:href (un-dotdot (str (or (-> ctx :request :context) "") (self-link ctx rel m)))
   :data (mapv transform-map m)})

(defhandler wrap-handler-collection-json
  "Wraps handler with a Collection+JSON handler. Note: consumes links;
  requires wrapping the Ring handler with octohipster.handlers/wrap-collection-json."
  ["application/vnd.collection+json"]
  (fn [hdlr ctx]
    (let [rsp (hdlr ctx)
          links (response-links-and-templates rsp)
          dk (:data-key rsp)
          result (dk rsp)
          items (if (map? result)
                  [(-> (cj-wrap ctx (name (or (:item-key ctx) :item)) result)
                       (assoc :links (-> links
                                         links-as-map
                                         (dissoc "self")
                                         (dissoc "listing")
                                         links-as-seq))
                       (assoc :href (:href (get links "self"))))]
                  (map (partial cj-wrap ctx dk) result))
          coll {:version "1.0"
                :href (if-let [up (get links "listing")]
                        (:href up)
                        (-> ctx :request :uri))
                :links (if (map? result) [] links)
                :items items}]
      {:encoder jsonify
       :body {:collection coll}})))
