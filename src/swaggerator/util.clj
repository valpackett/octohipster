(ns swaggerator.util
  (:require [clojure.string :as string]))

(defn concatv [& xs] (into [] (apply concat xs)))

(defn swaggerify-url-template [x]
  (string/replace x #":([^/]+)" "{$1}"))
