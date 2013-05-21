(ns octohipster.routes
  (:use [ring.middleware params keyword-params nested-params jsonp]
        [octohipster.documenters schema]
        [octohipster.params core json cj edn yaml]
        [octohipster.link header middleware]
        [octohipster.handlers util json edn yaml]
        [octohipster core problems host util]))

(defn routes
  "Creates a Ring handler that routes requests to provided groups
  and documenters."
  [& body]
  (let [defaults {:params [json-params collection-json-params yaml-params edn-params]
                  :documenters [schema-doc schema-root-doc]
                  :groups []
                  :problems {:resource-not-found {:status 404
                                                  :title "Resource not found"}
                             :invalid-data {:status 422
                                            :title "Invalid data"}}}
        options (merge defaults (apply hash-map body))
        {:keys [documenters groups params]} options
        problems (merge (:problems defaults) (:problems options))
        resources (mapcat :resources (gen-groups groups))
        raw-resources (mapcat :resources groups)
        docgen (partial gen-doc-resource
                        (-> options
                            (dissoc :documenters)
                            (assoc :resources raw-resources)))
        resources (concat resources (map docgen documenters))]
    (-> resources gen-handler
        ; Links
        wrap-add-self-link
        wrap-link-header
        ; Params
        (wrap-params-formats params)
        wrap-keyword-params
        wrap-nested-params
        wrap-params
        ; Response
        (wrap-expand-problems problems)
        (wrap-fallback-negotiation [wrap-handler-json wrap-handler-edn wrap-handler-yaml])
        wrap-apply-encoder
        wrap-expand-problem-ctype
        ; Headers, bindings, etc.
        wrap-cors
        wrap-json-with-padding
        wrap-context-bind
        wrap-host-bind)))

(defmacro defroutes
  "Creates a Ring handler (see routes) and defines a var with it."
  [n & body] `(def ~n (routes ~@body)))
