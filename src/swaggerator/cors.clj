(ns swaggerator.cors)

(defn wrap-cors [handler]
  (fn [req]
    (let [rsp (handler req)]
      (assoc rsp :headers (merge (-> rsp :headers)
                                 {"Access-Control-Allow-Origin" "*"
                                  "Access-Control-Allow-Headers" "Accept, Authorization, Content-Type"
                                  "Access-Control-Allow-Methods" "GET, POST, DELETE, PUT"})))))
