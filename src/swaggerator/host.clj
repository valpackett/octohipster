(ns swaggerator.host)

(def ^:dynamic *host* "")

(defn wrap-host-bind [handler]
  (fn [req]
    (binding [*host* (str (-> req :scheme name)
                          "://"
                          (or (get-in req [:headers "host"])
                              (-> req :server-name)))]
      (handler req))))
