(ns octohipster.handlers.json
  (:use [octohipster.handlers util]
        [octohipster json]))

(defhandler wrap-handler-json
  "Wraps a handler with a JSON handler."
  ["application/json"]
  (make-handler-fn jsonify))
