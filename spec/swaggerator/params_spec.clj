(ns swaggerator.params-spec
  (:require [clj-msgpack.core :as mp])
  (:use [speclj core]
        [ring.mock request]
        [swaggerator params]))

(defn app [req] (select-keys req [:non-query-params :params]))

(describe "wrap-json-params"
  (it "appends params to :non-query-params and :params"
    (should= {:non-query-params {:a 1}
              :params {:a 1}}
             ((-> app wrap-json-params)
              (-> (request :post "/")
                  (content-type "application/json")
                  (body "{\"a\":1}"))))))

(describe "wrap-yaml-params"
  (it "appends params to :non-query-params and :params"
    (doseq [ctype ["application/yaml" "application/x-yaml"
                   "text/yaml" "text/x-yaml"]]
      (should= {:non-query-params {:a 1}
                :params {:a 1}}
               ((-> app wrap-yaml-params)
                (-> (request :post "/") (content-type ctype) (body "{a: 1}")))))))

(describe "wrap-msgpack-params"
  (it "appends params to :non-query-params and :params"
    (should= {:non-query-params {"a" 1}
              :params {"a" 1}}
             ((-> app wrap-msgpack-params)
              (-> (request :post "/")
                  (content-type "application/x-msgpack")
                  (assoc :body (java.io.ByteArrayInputStream. (mp/pack {"a" 1}))))))))

(describe "wrap-edn-params"
  (it "appends params to :non-query-params and :params"
    (should= {:non-query-params {:a 1}
              :params {:a 1}}
             ((-> app wrap-edn-params)
              (-> (request :post "/")
                  (content-type "application/edn")
                  (body "{:a 1}")))))
  (it "does not evaluate clojure"
    (should= {:non-query-params {:a '(+ 1 2)}
              :params {:a '(+ 1 2)}}
             ((-> app wrap-edn-params)
              (-> (request :post "/")
                  (content-type "application/edn")
                  (body "{:a (+ 1 2)}"))))))
