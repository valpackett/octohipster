(ns octohipster.host)

(def ^:dynamic *host*
  "Current HTTP Host"
  "")

(defn wrap-host-bind
  "Ring middleware that wraps the handler in a binding
  that sets *host* to the HTTP Host header or :server-name
  if there's no Host header."
  [handler]
  (fn [req]
    (binding [*host* (str (-> req :scheme name)
                          "://"
                          (or (get-in req [:headers "host"])
                              (-> req :server-name)))]
      (handler req))))

(def ^:dynamic *context*
  "Current URL prefix (:context)"
  "")

(defn wrap-context-bind
  "Ring middleware that wraps the handler in a binding
  that sets *context*."
  [handler]
  (fn [req]
    (binding [*context* (:context req)]
      (handler req))))
