(ns octohipster.documenters.swagger
  (:use [octohipster core host mixins])
  (:require [clojure.string :as string]))

(def api-version "1.0")
(def swagger-version "1.1")

(defn swagger-root [groups]
  {:apiVersion api-version
   :swaggerVersion swagger-version
   :basePath (str *host* *context*)
   :apis (map (fn [g] {:path (:url g), :description (:desc g)}) groups)})

(defn swagger-root-doc [options]
  (resource
    :url "/api-docs.json"
    :mixins [handled-resource]
    :exists? (fn [ctx] {:data (swagger-root (:groups options))})))

(defn doc->operation [res [k v]]
  (-> v
      (assoc :httpMethod (string/upper-case (name k)))
      (assoc :responseClass
             (or (:responseClass v)
                 (let [id (-> res :schema :id)]
                   (if (and (= k :get) (:is-multiple? res))
                     (str "Array[" id "]")
                     id))))))

(defn resource->api [group res]
  {:path (str (:url group) (:url res))
   :description (:desc res)
   :operations (map (partial doc->operation res) (:doc res))})

(defn resource->model [{:keys [schema]}]
  [(keyword (:id schema)) schema])

(defn swagger-api-decl [groups path]
  (let [group (->> groups
                   (filter #(= (:url %) path))
                   first)
        resources (:resources group)]
    {:apiVersion api-version
     :swaggerVersion swagger-version
     :basePath (str *host* *context*)
     :resourcePath (:url group)
     :apis (map (partial resource->api group) resources)
     :models (into {} (map resource->model resources))}))

(defn swagger-doc [options]
  (resource
    :url "/api-docs.json/{path}"
    :mixins [handled-resource]
    :exists? (fn [ctx] {:data (swagger-api-decl (:groups options)
                                                (str "/" (-> ctx :request :route-params :path)))})))
