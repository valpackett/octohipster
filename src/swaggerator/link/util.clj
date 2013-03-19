(ns swaggerator.link.util
  (:use [swaggerator util])
  (:require [clojure.string :as string]))

(defn un-dotdot [x]
  (string/replace x #"/[^/]+/\.\." ""))

(defn prepend-to-href [uri-context l]
  (assoc l :href (un-dotdot (str uri-context (:href l)))))

(defn response-links-and-templates [rsp]
  (concatv
    (:links rsp)
    (map #(assoc % :templated true) (:link-templates rsp))))

(defn links-as-map [l]
  (into {}
    (map (fn [x] [(:rel x) (-> x (dissoc :rel))]) l)))

(defn links-as-seq [l]
  (mapv (fn [[k v]] (assoc v :rel k)) l))
