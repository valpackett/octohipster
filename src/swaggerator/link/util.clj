(ns swaggerator.link.util
  (:use [swaggerator util])
  (:require [clojure.string :as string]))

(defn un-dotdot [x]
  (string/replace x #"/[^/]+/\.\." ""))

(defn prepend-to-href [uri-context l]
  (assoc l :href (un-dotdot (str uri-context (:href l)))))

(defn links-as-map [rsp]
  (into {}
    (concatv
      (map (fn [x] [(:rel x) (-> x (dissoc :rel))]) (:links rsp))
      (map (fn [x] [(:rel x) (-> x (dissoc :rel) (assoc :templated true))]) (:link-templates rsp)))))
