(ns swaggerator.pagination
  (:use [swaggerator util]))

(def ^:dynamic *skip*
  "Skip parameter for database queries" 0)
(def ^:dynamic *limit*
  "Limit parameter for database queries" 0)

(defn wrap-pagination
  "Ring middleware that calculates skip and limit database parameters based on
  a counter function and request parameters, sets *skip* and *limit* to these values,
  adds first/prev/next/last links to the :links parameter in the response (for
  swaggerator.link/wrap-link-header, swaggerator.handlers/wrap-hal-json
  or any other middleware that consument :links)."
  [handler {counter :counter
            default-pp :default-per-page}]
  (fn [req]
    (let [page (if-let [pparam (get-in req [:query-params "page"])]
                 (Integer/parseInt pparam)
                 1)
          per-page (if-let [pparam (get-in req [:query-params "per_page"])]
                     (Integer/parseInt pparam)
                     default-pp)
          last-page (-> (/ (counter req) per-page) Math/ceil int)
          last-page (if (== 0 last-page) 1 last-page)
          prev-links (if (not= 1 page)
                       [{:rel "first" :href (uri-alter-query-params req {"page" 1})}
                        {:rel "prev"  :href (uri-alter-query-params req {"page" (- page 1)})}]
                       [])
          next-links (if (not= last-page page)
                       [{:rel "next"  :href (uri-alter-query-params req {"page" (+ page 1)})}
                        {:rel "last"  :href (uri-alter-query-params req {"page" last-page})}]
                       [])]
      (binding [*skip* (* per-page (- page 1))
                *limit* per-page]
        (handler (assoc req :links
                        (concatv (:links req) prev-links next-links)))))))
