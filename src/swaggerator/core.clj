(ns swaggerator.core
  "Functions and macros for building REST APIs through
  creating resources, controllers and routes."
  (:require [liberator.core :as lib]
            [clout.core :as clout]
            [clojure.string :as string])
  (:use [ring.middleware params keyword-params nested-params]
        [swaggerator.link header middleware]
        [swaggerator host util]))

(defn resource
  "Creates a resource. Basically, compiles a map from arguments."
  [& body] (apply hash-map body))

(defmacro defresource
  "Creates a resource and defines a var with it,
  adding the var under :id as a namespace-qualified keyword."
  [n & body] `(def ~n (resource ~@body :id ~(keyword (str *ns* "/" n)))))

(defn controller
  "Creates a controller, adding everything from :add-to-resources to all
  resources and applying mixins to them."
  [& body]
  (let [c (apply hash-map body)
        c (-> c
              (assoc :resources
                     (map (comp (fn [r] (reduce #(%2 %1) (dissoc r :mixins) (:mixins r)))
                                (partial merge (:add-to-resources c))) (:resources c)))
              (dissoc :add-to-resources))]
    c))

(defmacro defcontroller
  "Creates a controller and defines a var with it."
  [n & body] `(def ~n (controller ~@body)))

(defn- wrap-all-the-things [handler]
  (-> handler
      wrap-add-self-link
      wrap-link-header
      wrap-host-bind
      wrap-cors
      wrap-keyword-params
      wrap-nested-params
      wrap-params))

(defn gen-resource [r]
  {:url (:url r)
   :handler (reduce #(%2 %1)
                    (apply-kw lib/resource r)
                    (conj (:middleware r) wrap-all-the-things))})

(defn all-resources [cs]
  (apply concat
    (map (fn [c] (map #(assoc % :url (str (:url c) (:url %))) (:resources c))) cs)))

(defn gen-controller [resources c]
  (-> c
    (assoc :resources
      (mapv
        (fn [r]
          (-> r
              (assoc :clinks
                (mapv (fn [[k v]]
                        [k (->> resources (filter #(= v (:id %))) first :url)])
                      (:clinks r)))
              gen-resource
              (assoc :route (-> (str (:url c) (:url r)) uri-template->clout clout/route-compile))
              (dissoc :url)))
        (:resources c)))))

(defn not-found-handler [req]
  {:status 404
   :headers {"Content-Type" "application/json"}
   :body "{\"error\":\"Not found\"}"})

(defn gen-controllers [c]
  (map (partial gen-controller (all-resources c)) c))

(defn gen-handler [resources not-found-handler]
  (fn [req]
    (let [h (->> resources
                 (map #(assoc % :match (clout/route-matches (:route %) req)))
                 (filter :match)
                 first)
          h (or h {:handler not-found-handler})]
      ((:handler h) (assoc req :route-params (:match h))))))

(defn gen-doc-resource [options d]
  (->> (controller :url "", :resources [(d options)])
       (gen-controller (:controllers options))
       :resources first))
