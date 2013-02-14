(ns swaggerator.core
  (:require [liberator.core :as lib]
            [compojure.core :as cmpj]
            [clojure.string :as string])
  (:use [ring.middleware params keyword-params nested-params]
        [swaggerator json host link validator pagination handlers util]
        (inflections core)))

(def ^:dynamic *url* (atom ""))
(def ^:dynamic *swagger-version* "1.1")
(def ^:dynamic *swagger-apis* (atom []))
(def ^:dynamic *swagger-schemas* (atom {}))
(def ^:dynamic *global-error-responses*
  [{:code 422
    :reason "The data did not pass schema validation"}
   {:code 404
    :reason "Resource not found"}])
(def ^:dynamic *global-parameters* [])

(def request-method-in lib/request-method-in)

(defn- resource->operations [doc]
  (mapv #(let [doc (-> doc %)]
           (-> doc
               (assoc :httpMethod (-> % name string/upper-case))
               (assoc :responseClass (or (-> doc :responseClass) "void"))
               (assoc :parameters (concatv (or (-> doc :parameters) [])
                                         *global-parameters*))
               (assoc :errorResponses (concatv (or (-> doc :errorResponses) [])
                                             *global-error-responses*))))
        (keys doc)))

(defmacro resource [desc & kvs]
  (let [k (apply hash-map kvs)
        link-tpls (-> k :link-templates eval)
        schema (-> k :schema eval)]
    (swap! *handled-content-types* (constantly []))
    (swap! *swagger-apis* conj
      {:path (-> @*url* eval swaggerify-url-template)
       :description (eval desc)
       :operations (-> k :doc eval resource->operations)})
    (swap! *swagger-schemas* assoc (-> schema :id) schema)
     `(-> (lib/resource ~@kvs
                        :available-media-types @*handled-content-types*)
          (wrap-json-schema-validator ~schema)
          ; add links:
          (wrap-add-link-templates ~link-tpls)
          wrap-add-self-link
          ; independent:
          wrap-handle-options-and-head)))

(defmacro listing-resource [desc & kvs]
  (let [k (apply hash-map kvs)
        ckey (-> k :children-key)
        rel (or (-> k :child-rel)
                (-> ckey name singular))]
    `(-> (resource ~desc
                   :method-allowed? (request-method-in :get :post)
                   :link-templates [{:href (:child-url-template ~k) :rel ~rel}]
                   :link-mapping {~ckey ~rel}
                   :handle-ok (default-list-handler (:presenter ~k) ~ckey)
                   :post-redirect? true
                   :see-other (params-rel ~rel)
                   ~@kvs)
         (wrap-pagination {:counter (:count ~k)
                           :default-per-page (:default-per-page ~k)}))))

(defmacro entry-resource [desc & kvs]
  (let [k (apply hash-map kvs)]
    `(resource ~desc
               :method-allowed? (request-method-in :get :put :delete)
               :respond-with-entity? true
               :new? false
               :can-put-to-missing? false
               :handle-ok (default-entry-handler (:presenter ~k) (:data-key ~k))
               ~@kvs)))

(defmacro route [url binds & body]
  (swap! *url* (constantly url))
  `(cmpj/ANY ~url ~binds
             (-> ~@body
                 ; consume links:
                 wrap-hal-json
                 wrap-link-header
                 ; independent:
                 wrap-host-bind
                 wrap-cors
                 wrap-json-params
                 wrap-keyword-params
                 wrap-nested-params
                 wrap-params)))

(defmacro controller [n url desc & body]
  (swap! *swagger-apis* (constantly []))
  (swap! *swagger-schemas* (constantly {}))
  `(with-meta
     (cmpj/routes ~@body)
     {:resourcePath ~url
      :name ~n
      :description ~desc
      :apis (map #(assoc % :path (str ~url (:path %))) @*swagger-apis*)
      :models @*swagger-schemas*}))

(defmacro defcontroller [n url desc & body]
  (let [nn (keyword n)]
    `(def ~n (controller ~nn ~url ~desc ~@body))))

(defn nest [x]
  (cmpj/context (-> x meta :resourcePath) [] x))

(defn- controller->listing-entry [x]
  {:path (str "/api-docs.json" (-> x meta :resourcePath))
   :description (-> x meta :description)})

(defn- swagger-controller-route [x]
  (let [m (meta x)]
    (cmpj/GET (-> m :resourcePath) []
      (serve-json (merge m {:swaggerVersion *swagger-version*
                            :basePath *host*})))))

(defn swagger-routes [& xs]
  (cmpj/context "/api-docs.json" []
    (-> (apply cmpj/routes
          (cmpj/GET "/" []
            (serve-json {:swaggerVersion *swagger-version*
                         :basePath *host*
                         :apis (map controller->listing-entry xs)}))
          (map swagger-controller-route xs))
        wrap-host-bind
        wrap-cors)))

(defn make-schema [xs]
  (apply merge (map (comp :models meta) xs)))

(defn schema-route [& xs]
  (cmpj/GET "/schema" []
    (serve-hal-json (make-schema xs))))

(defn root-route [& xs]
  (cmpj/GET "/" []
    (serve-hal-json
      {:_links (into {}
                     (mapv (fn [x] [(-> x meta :name)
                                    {:href  (-> x meta :resourcePath)
                                     :title (-> x meta :description)}]) xs))
       :_embedded {:schema (assoc (make-schema xs) :_links {:self {:href "/schema"}})}})))

(defmacro defroutes [n & xs]
  `(cmpj/defroutes ~n
     ~@(mapv (fn [x] `(nest ~x)) xs) ; call nest for every x in xs
     (swagger-routes ~@xs)
     (schema-route ~@xs)
     (root-route ~@xs)))

(defn params-rel [rel]
  (fn [ctx]
    (let [tpl (uri-template-for-rel ctx rel)]
      (expand-uri-template tpl (-> ctx :request :params)))))
