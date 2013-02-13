(ns swaggerator.link-spec
  (:use [speclj core]
        [ring.mock request]
        [swaggerator link]))

(describe "make-link-header"
  (it "makes the link header"
    (should= (make-link-header []) "")
    (should= (make-link-header [{:href "/hello" :rel "next"}])
             "</hello>; rel=\"next\"")
    (should= (make-link-header [{:href "/hello" :rel "next"}
                                {:href "/olleh" :rel "txen"}])
             "</hello>; rel=\"next\", </olleh>; rel=\"txen\"")))

(describe "wrap-link-header"
  (it "does not use make-link-header when there are no :links"
    (let [app (wrap-link-header (fn [req] {:status 200 :headers {} :body ""}))]
      (with-redefs [make-link-header (constantly "test")]
        (should= (get (-> (request :get "/") app :headers) "Link") nil))))
  (it "uses make-link-header when there are :links"
    (let [app (wrap-link-header (fn [req] {:status 200 :headers {} :links [] :body ""}))]
      (with-redefs [make-link-header (constantly "test")]
        (should= (get (-> (request :get "/") app :headers) "Link") "test")))))
