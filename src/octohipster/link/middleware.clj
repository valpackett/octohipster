(ns octohipster.link.middleware
  (:use [octohipster util]
        [octohipster.link util]))

(defn wrap-add-links-1 [handler links k]
  (fn [req]
    (handler (assoc req k (concatv (or (k req) []) links)))))

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
    (handler
      (assoc req :links
             (concatv (or (:links req) [])
                      [{:href (context-relative-uri req)
                        :rel "self"}])))))
