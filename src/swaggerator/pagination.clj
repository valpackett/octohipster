(ns swaggerator.pagination
  (:use [swaggerator util]))

(defn wrap-pagination [handler {counter :counter
                                default-pp :default-per-page}]
  (fn [req]
    (let [page (if-let [pparam (get-in req [:query-params "page"])]
                 (Integer/parseInt pparam)
                 1)
          per-page (if-let [pparam (get-in req [:query-params "per_page"])]
                     (Integer/parseInt pparam)
                     default-pp)
          last-page (-> (/ (counter) per-page) Math/ceil int)
          prev-links (if (not= 1 page)
                       [{:rel "first" :href (uri-alter-query-params req {"page" 1})}
                        {:rel "prev"  :href (uri-alter-query-params req {"page" (- page 1)})}]
                       [])
          next-links (if (not= last-page page)
                       [{:rel "next"  :href (uri-alter-query-params req {"page" (+ page 1)})}
                        {:rel "last"  :href (uri-alter-query-params req {"page" last-page})}]
                       [])
          rsp (-> req
                  (assoc :pagination {:limit per-page
                                      :skip (* per-page (- page 1))})
                  handler)]
      (assoc rsp :links
             (concatv (:links rsp) prev-links next-links)))))
