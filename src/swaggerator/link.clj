(ns swaggerator.link)

(defn make-link-header-field [[k v]]
  (format "%s=\"%s\"" (name k) v))

(defn make-link-header-element [link]
  (let [fields (map make-link-header-field (dissoc link :href))]
    (format "<%s>%s"
            (:href link)
            (if (not (empty? fields))
              (->> fields
                   (interpose " ")
                   (apply str "; "))
              ""))))

(defn make-link-header [links]
  (if (empty? links) nil
    (->> links
         (map make-link-header-element)
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
