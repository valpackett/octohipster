(ns swaggerator.link
  (:use [swaggerator util]))

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

(defn- wrap-link-header-1 [handler k h]
  (fn [req]
    (let [rsp (-> req
                  (assoc k (or (k req) []))
                  handler)]
      (-> rsp
          (assoc-in [:headers h] (-> rsp k make-link-header))
          (dissoc k)))))

(defn wrap-link-header [handler]
  (-> handler
      (wrap-link-header-1 :links "Link")
      (wrap-link-header-1 :link-templates "Link-Template")))

(defn wrap-add-link-templates [handler tpls]
  (fn [req]
    (let [rsp (handler req)]
      (assoc rsp :link-templates
             (concatv (or (:link-templates rsp) []) tpls)))))
