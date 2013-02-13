(ns swaggerator.link-spec
  (:use [speclj core]
        [ring.mock request]
        [swaggerator link]))

(describe "make-link-header"
  (it "makes the link header"
    (should= (make-link-header []) nil)
    (should= (make-link-header [{:href "/hello" :rel "next"}])
             "</hello>; rel=\"next\"")
    (should= (make-link-header [{:href "/hello" :rel "next"}
                                {:href "/test" :rel "root" :title "thingy"}])
             "</hello>; rel=\"next\", </test>; title=\"thingy\" rel=\"root\"")))

(describe "wrap-link-header"
  (it "does not return the header when there are no :links"
    (let [app (wrap-link-header (fn [req] {:status 200 :headers {} :links [] :body ""}))]
      (should= (get (-> (request :get "/") app :headers) "Link") nil)))
  (it "returns the header when there are :links"
    (let [app (wrap-link-header (fn [req] {:status 200 :headers {} :links [{:rel "a" :href "b"}] :body ""}))]
      (should= (get (-> (request :get "/") app :headers) "Link") "<b>; rel=\"a\""))))
