(ns octohipster.params.edn
  (:require [clojure.edn :as edn]))

(def edn-params
  "EDN params support"
  ^{:ctype-re #"^application/(vnd.+)?edn"}
  (fn [body]
    (edn/read-string body)))
