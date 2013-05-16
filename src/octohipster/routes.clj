(ns octohipster.routes
  (:use [ring.middleware params keyword-params nested-params jsonp]
        [octohipster.documenters schema]
        [octohipster.params core json edn yaml]
        [octohipster.link header middleware]
        [octohipster.handlers util]
        [octohipster core host util]))

(defn routes
  "Creates a Ring handler that routes requests to provided groups
  and documenters, using params handers and not-found-handler."
  [& body]
  (let [defaults {:not-found-handler not-found-handler
                  :params [json-params yaml-params edn-params]
                  :documenters [schema-doc schema-root-doc]
                  :groups []}
        options (merge defaults (apply hash-map body))
        {:keys [documenters groups params not-found-handler]} options
        resources (mapcat :resources (gen-groups groups))
        raw-resources (mapcat :resources groups)
        docgen (partial gen-doc-resource
                        (-> options
                            (dissoc :documenters)
                            (assoc :resources raw-resources)))
        resources (concat resources
                          (map docgen documenters))]
    (-> (gen-handler resources not-found-handler)
        ; Links
        wrap-add-self-link
        wrap-link-header
        ; Params
        (wrap-params-formats params)
        wrap-keyword-params
        wrap-nested-params
        wrap-params
        ; Response
        wrap-apply-encoder
        ; Headers, bindings, etc.
        wrap-cors
        wrap-json-with-padding
        wrap-context-bind
        wrap-host-bind)))

(defmacro defroutes
  "Creates a Ring handler (see routes) and defines a var with it."
  [n & body] `(def ~n (routes ~@body)))
