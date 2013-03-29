(ns octohipster.documenters.schema
  (:use [octohipster core mixins])
  (:require [clojure.string :as string]))

(defn- schema-from-res [res]
  (let [schema (:schema res)]
    [(-> schema :id keyword) schema]))

(defn- schema-from-options [options]
  (->> options :resources
       (map schema-from-res)
       (into {})))

(defn schema-doc [options]
  (resource
    :url "/schema"
    :mixins [handled-resource]
    :exists? (fn [ctx] {:data (schema-from-options options)})))

(defn- links-for-groups [options]
  (->> options :groups
       (map :url)
       (map (fn [c] {:rel (string/replace c "/" "") :href c}))))

(defn schema-root-doc [options]
  (resource
    :url "/"
    :mixins [handled-resource]
    :exists? (fn [ctx]
               {:links (links-for-groups options)
                :_embedded {:schema (assoc (schema-from-options options) :_links {:self {:href "/schema"}})}})))
