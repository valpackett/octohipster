(ns swaggerator.link)

(defn make-link-header [links]
  (if (empty? links) nil
    (->> links
         (map #(format "<%s>; rel=\"%s\"" (:href %) (:rel %)))
         (interpose ", ")
         (apply str))))

(defn wrap-link-header [handler]
  (fn [req]
    (let [rsp (-> req
                  (assoc :links (or (:links req) []))
                  handler)]
      (-> rsp
          (assoc-in [:headers "Link"] (-> rsp :links make-link-header))
          (dissoc :links)))))
