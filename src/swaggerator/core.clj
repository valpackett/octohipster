(ns swaggerator.core
  (:require [liberator.core :as lib]
            [compojure.core :as cmpj]
            [clojure.string :as string])
  (:use [ring.middleware params keyword-params nested-params]
        [swaggerator json host cors link validator util]))

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
        schema (-> k :schema eval)]
    (swap! *swagger-apis* conj
      {:path (-> @*url* eval swaggerify-url-template)
       :description (eval desc)
       :operations (-> k :doc eval resource->operations)})
    (swap! *swagger-schemas* assoc (-> schema :id) schema)
     `(-> (lib/resource ~@kvs)
          (wrap-json-schema-validator ~schema))))

(defmacro route [url binds & body]
  (swap! *url* (constantly url))
  `(cmpj/ANY ~url ~binds ~@body))

(defmacro controller [url desc & body]
  (swap! *swagger-apis* (constantly []))
  (swap! *swagger-schemas* (constantly {}))
  `(with-meta
     (-> (cmpj/routes ~@body)
         wrap-host-bind
         wrap-cors
         wrap-link-header
         wrap-json-params
         wrap-keyword-params
         wrap-nested-params
         wrap-params)
     {:resourcePath ~url
      :description ~desc
      :apis (map #(assoc % :path (str ~url (:path %))) @*swagger-apis*)
      :models @*swagger-schemas*}))

(defmacro defcontroller [n url desc & body]
  `(def ~n (controller ~url ~desc ~@body)))

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
