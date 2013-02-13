(ns swaggerator.link)

(defn make-link-header [links]
  (->> links
       (map #(format "<%s>; rel=\"%s\"" (:href %) (:rel %)))
       (interpose ", ")
       (apply str)))

(defn wrap-link-header [handler]
  (fn [req]
    (let [rsp (handler req)]
      (if-let [links (:links rsp)]
        (-> rsp
            (assoc-in [:headers "Link"] (make-link-header links))
            (dissoc :links))
        rsp))))
