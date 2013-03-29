(ns octohipster.params.core)

(defn wrap-params-formats [handler formats]
  (fn [req]
    (if-let [#^String ctype (:content-type req)]
      (if-let [f (->> formats
                      (filter #(not (empty? (re-find (:ctype-re (meta %)) ctype))))
                      first)]
        (let [params (-> req :body slurp f)
              req* (assoc req
                          :non-query-params (merge (:non-query-params req) params)
                          :params (merge (:params req) params))]
          (handler req*))
        (handler req))
      (handler req))))
