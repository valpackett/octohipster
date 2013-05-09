(ns octohipster.handlers.middleware
  "Ring middleware for modifying the response before it gets
  encoded into the serialization format passed by handlers.")

(defn wrap-response-envelope [handler]
  (fn [req]
    (let [rsp (handler req)]
      (if-let [k (:data-key rsp)]
        (if-not (:body-no-envelope? rsp)
          (assoc rsp :body {k (:body rsp)})
          rsp)
        rsp))))
