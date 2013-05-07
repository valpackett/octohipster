(ns octohipster.handlers.edn
  (:use [octohipster.handlers util]))

(defhandler wrap-handler-edn
  "Wraps a handler with a EDN handler."
  ["application/edn"]
  (make-handler-fn pr-str))
