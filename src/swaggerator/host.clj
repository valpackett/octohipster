(ns swaggerator.host)

(def ^:dynamic *host* "")

(defn wrap-host-bind [handler]
  (fn [req]
    (binding [*host* (str (-> req :scheme name) "://" (-> req :remote-addr))]
      (handler req))))
