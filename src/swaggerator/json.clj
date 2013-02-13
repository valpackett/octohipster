(ns swaggerator.json
  (:require [cheshire.core :as json]))

; thanks: https://github.com/mmcgrana/ring-json-params/blob/master/src/ring/middleware/json_params.clj

(defn- json-request?
  [req]
  (if-let [#^String type (:content-type req)]
    (not (empty? (re-find #"^application/(vnd.+)?json" type)))))
 
(defn wrap-json-params [handler]
  (fn [req]
    (if-let [body (and (json-request? req) (:body req))]
      (let [bstr (slurp body)
            json-params (json/parse-string bstr true)
            req* (assoc req
                   :json-params json-params
                   :params (merge (:params req) json-params))]
        (handler req*))
      (handler req))))

(defn jsonify [x] (json/generate-string x))

(defn serve-json [x]
  (fn [req]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (jsonify x)}))

(defn wrap-handler-json
  ([handler] (wrap-handler-json handler :data))
  ([handler k]
    (fn [ctx]
      (case (-> ctx :representation :media-type)
        "application/json" (-> ctx handler k jsonify)
        (handler ctx)))))
