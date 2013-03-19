(ns swaggerator.core
  "Functions and macros for building REST APIs through
  creating resources, controllers and routes."
  (:require [liberator.core :as lib]
            [clout.core :as clout]
            [clojure.string :as string])
  (:use [ring.middleware params keyword-params nested-params]
        [swaggerator.link header]
        [swaggerator params host util]))

(defn resource [& body]
  (let [r (apply hash-map body)
        r (reduce #(%2 %1) r (or (:mixins r) []))]
    r))

(defmacro defresource [n & body]
  `(def ~n (resource ~@body :id ~(keyword (str *ns* "/" n)))))

(defn controller [& body]
  (let [c (apply hash-map body)
        c (-> c
              (assoc :resources
                     (map (partial merge (:add-to-resources c)) (:resources c)))
              (dissoc :add-to-resources))]
    c))

(defmacro defcontroller [n & body]
  `(def ~n (controller ~@body)))

(defn- wrap-all-the-things [handler]
  (-> handler
      wrap-link-header
      wrap-host-bind
      wrap-cors
      wrap-json-params
      wrap-edn-params
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
              (assoc :route (clout/route-compile (str (:url c) (:url r))))
              (dissoc :url)))
        (:resources c)))))

(defn not-found-handler [req]
  {:status 404
   :headers {"Content-Type" "application/json"}
   :body "{\"error\":\"Not found\"}"})

(defn routes [& body]
  (let [defaults {:not-found-handler not-found-handler
                  :controllers []}
        a (merge defaults (apply hash-map body))
        ctrs (map (partial gen-controller (all-resources (:controllers a)))
                  (:controllers a))
        rts (apply concat (map :resources ctrs))]
    (fn [req]
      (let [h (->> rts
                   (map #(assoc % :match (clout/route-matches (:route %) req)))
                   (filter :match)
                   first)
            h (or h {:handler (:not-found-handler a)})]
        ((:handler h) (assoc req :route-params (:match h)))))))

(defmacro defroutes [n & body]
  `(def ~n (routes ~@body)))
