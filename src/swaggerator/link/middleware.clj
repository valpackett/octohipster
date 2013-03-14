(ns swaggerator.link.middleware
  (:use [swaggerator util]
        [swaggerator.link util]))

(defn wrap-add-links-1 [handler links k]
  (fn [req]
    (let [rsp (handler req)]
      (assoc rsp k (concatv (or (k rsp) []) links)))))

(defn wrap-add-link-templates
  "Ring middleware that adds specified templates to :link-templates."
  [handler tpls] (wrap-add-links-1 handler tpls :link-templates))

(defn wrap-add-links
  "Ring middleware that adds specified links to :links."
  [handler links] (wrap-add-links-1 handler links :links))

(defn wrap-add-self-link
  "Ring middleware that adds a link to the requested URI as rel=self to :links."
  [handler]
  (fn [req]
    (let [rsp (handler req)]
      (assoc rsp :links
             (concatv (or (:links rsp) [])
                      [{:href (context-relative-uri req)
                        :rel "self"}])))))

(defn wrap-context-relative-links [handler]
  (fn [req]
    (let [uri-context (or (:context req) "")
          prepender (partial prepend-to-href uri-context)
          rsp (handler req)]
      (-> rsp
          (assoc :links (map prepender (:links rsp)))
          (assoc :link-templates (map prepender (:link-templates rsp)))))))
