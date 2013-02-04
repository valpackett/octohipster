(ns swaggerator.core
  (:require [liberator.core :as lib]
            [compojure.core :as cmpj]
            [clojure.string :as string])
  (:use [ring.middleware params keyword-params nested-params]
        [swaggerator json host cors util]))

(def ^:dynamic *controller-url* nil)
(def ^:dynamic *swagger-version* "1.1")
(def ^:dynamic *swagger-apis* nil)
(def ^:dynamic *swagger-schemas* nil)
(def ^:dynamic *global-error-responses* [])
(def ^:dynamic *global-parameters* [])

(defn- http-methods [kvs]
  (filter identity (map #{:get :post :put :delete} (-> kvs :method-allowed?))))

(defn- resource->operations [kvs]
  (mapv #(let [doc (-> kvs :doc %)]
           (-> doc
               (assoc :httpMethod (-> % name string/upper-case))
               (assoc :responseClass (or (-> doc :responseClass) "void"))
               (assoc :parameters (concatv (or (-> doc :parameters) [])
                                         *global-parameters*))
               (assoc :errorResponses (concatv (or (-> doc :errorResponses) [])
                                             *global-error-responses*))))
        (http-methods kvs)))

(defmacro resource [url binds desc & kvs]
  (let [k (apply hash-map kvs)
        schema (eval (-> k :schema))]
    (conj! *swagger-apis*
      {:path (str *controller-url* (swaggerify-url-template url))
       :description desc
       :operations (resource->operations k)})
    (assoc! *swagger-schemas* (-> schema :id) schema)
    `(cmpj/ANY ~url ~binds
       (lib/resource ~@kvs))))

(defmacro defcontroller [n url desc & body]
  (intern *ns* n
    (binding [*controller-url* url
              *swagger-apis* (transient [])
              *swagger-schemas* (transient {})]
      (with-meta
        (eval `(-> (cmpj/routes ~@body)
                    wrap-host-bind
                    wrap-cors
                    wrap-json-params
                    wrap-keyword-params
                    wrap-nested-params
                    wrap-params))
        {:resourcePath url
         :description desc
         :apis (persistent! *swagger-apis*)
         :models (persistent! *swagger-schemas*)}))))

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
