(ns swaggerator.core
  "Functions and macros for building REST APIs through
  creating resources, controllers and routes."
  (:require [liberator.core :as lib]
            [clout.core :as clout]
            [clojure.string :as string])
  (:use [ring.middleware params keyword-params nested-params]
        [swaggerator.link header]
        [swaggerator.params core json edn yaml]
        [swaggerator host util]))

(defn resource [& body] (apply hash-map body))

(defmacro defresource [n & body]
  `(def ~n (resource ~@body :id ~(keyword (str *ns* "/" n)))))

(defn controller [& body]
  (let [c (apply hash-map body)
        c (-> c
              (assoc :resources
                     (map (comp (fn [r] (reduce #(%2 %1) (dissoc r :mixins) (:mixins r)))
                                (partial merge (:add-to-resources c))) (:resources c)))
              (dissoc :add-to-resources))]
    c))

(defmacro defcontroller [n & body]
  `(def ~n (controller ~@body)))

(defn- wrap-all-the-things [handler]
  (-> handler
      wrap-link-header
      wrap-host-bind
      wrap-cors
      wrap-keyword-params
      wrap-nested-params
      wrap-params))

(defn- gen-resource [r]
  {:url (:url r)
   :handler (reduce #(%2 %1)
                    (apply-kw lib/resource r)
                    (conj (:middleware r) wrap-all-the-things))})

(defn- all-resources [cs]
  (apply concat
    (map (fn [c] (map #(assoc % :url (str (:url c) (:url %))) (:resources c))) cs)))

(defn- gen-controller [resources c]
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

(defn- gen-controllers [c]
  (map (partial gen-controller (all-resources c)) c))

(defn- gen-handler [resources not-found-handler]
  (fn [req]
    (let [h (->> resources
                 (map #(assoc % :match (clout/route-matches (:route %) req)))
                 (filter :match)
                 first)
          h (or h {:handler not-found-handler})]
      ((:handler h) (assoc req :route-params (:match h))))))

(defn- gen-doc-resource [options d]
  (->> (controller :url "", :resources [(d options)])
       (gen-controller (:controllers options))
       :resources first))

(defn routes [& body]
  (let [defaults {:not-found-handler not-found-handler
                  :params [json-params yaml-params edn-params]
                  :documenters []
                  :controllers []}
        options (merge defaults (apply hash-map body))
        resources (apply concat (map :resources (gen-controllers (:controllers options))))
        raw-resources (apply concat (map :resources (:controllers options)))
        options-for-doc (-> options
                            (dissoc :documenters)
                            (assoc :resources raw-resources))
        resources (concat resources
                          (map (partial gen-doc-resource options-for-doc)
                               (:documenters options)))]
    (-> (gen-handler resources (:not-found-handler options))
        (wrap-params-formats (:params options)))))

(defmacro defroutes [n & body]
  `(def ~n (routes ~@body)))
