(ns octohipster.params-spec
  (:use [speclj core]
        [ring.mock request]
        [octohipster.params core json yaml edn]))

(defn app [req] (select-keys req [:non-query-params :params]))

(describe "json-params"
  (it "appends params to :non-query-params and :params"
    (should= {:non-query-params {:a 1}
              :params {:a 1}}
             ((wrap-params-formats app [json-params])
              (-> (request :post "/")
                  (content-type "application/json")
                  (body "{\"a\":1}"))))))

(describe "yaml-params"
  (it "appends params to :non-query-params and :params"
    (doseq [ctype ["application/yaml" "application/x-yaml"
                   "text/yaml" "text/x-yaml"]]
      (should= {:non-query-params {:a 1}
                :params {:a 1}}
               ((wrap-params-formats app [yaml-params])
                (-> (request :post "/")
                    (content-type ctype)
                    (body "{a: 1}")))))))

(describe "edn-params"
  (it "appends params to :non-query-params and :params"
    (should= {:non-query-params {:a 1}
              :params {:a 1}}
             ((wrap-params-formats app [edn-params])
              (-> (request :post "/")
                  (content-type "application/edn")
                  (body "{:a 1}")))))

  (it "does not evaluate clojure"
    (should= {:non-query-params {:a '(+ 1 2)}
              :params {:a '(+ 1 2)}}
             ((wrap-params-formats app [edn-params])
              (-> (request :post "/")
                  (content-type "application/edn")
                  (body "{:a (+ 1 2)}"))))))
