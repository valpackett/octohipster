(ns octohipster.routes
  (:use [octohipster.documenters schema]
        [octohipster.params core json edn yaml]
        [octohipster core]))

(defn routes
  "Creates a Ring handler that routes requests to provided groups
  and documenters, using params handers and not-found-handler."
  [& body]
  (let [defaults {:not-found-handler not-found-handler
                  :params [json-params yaml-params edn-params]
                  :documenters [schema-doc schema-root-doc]
                  :groups []}
        options (merge defaults (apply hash-map body))
        resources (apply concat (map :resources (gen-groups (:groups options))))
        raw-resources (apply concat (map :resources (:groups options)))
        options-for-doc (-> options
                            (dissoc :documenters)
                            (assoc :resources raw-resources))
        resources (concat resources
                          (map (partial gen-doc-resource options-for-doc)
                               (:documenters options)))]
    (-> (gen-handler resources (:not-found-handler options))
        (wrap-params-formats (:params options)))))

(defmacro defroutes
  "Creates a Ring handler (see routes) and defines a var with it."
  [n & body] `(def ~n (routes ~@body)))
