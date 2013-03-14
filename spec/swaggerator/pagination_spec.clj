(ns swaggerator.pagination-spec
  (:use [swaggerator pagination]
        [swaggerator.link header]
        [ring.middleware params]
        [ring.mock request]
        [speclj core]))

(defn app [req]
  {:status 200
   :headers {}
   :body (str {:limit *limit* :skip *skip*})})

(def app-with-zero
  (-> app
      (wrap-pagination {:counter (constantly 0)
                        :default-per-page 5})
      wrap-link-header
      wrap-params))

(def app-1-page
  (-> app
      (wrap-pagination {:counter (constantly 4)
                        :default-per-page 5})
      wrap-link-header
      wrap-params))

(def app-2-pages
  (-> app
      (wrap-pagination {:counter (constantly 20)
                        :default-per-page 10})
      wrap-link-header
      wrap-params))

(def app-4-pages
  (-> app
      (wrap-pagination {:counter (constantly 20)
                        :default-per-page 5})
      wrap-link-header
      wrap-params))

(describe "wrap-pagination"
  (it "gives skip and limit parameters to the app"
    (let [rsp (-> (request :get "/") app-1-page :body)]
      (should= rsp "{:limit 5, :skip 0}"))
    (let [rsp (-> (request :get "/") app-2-pages :body)]
      (should= rsp "{:limit 10, :skip 0}"))
    (let [rsp (-> (request :get "/?page=2") app-2-pages :body)]
      (should= rsp "{:limit 10, :skip 10}"))
    (let [rsp (-> (request :get "/?page=2") app-4-pages :body)]
      (should= rsp "{:limit 5, :skip 5}"))
    (let [rsp (-> (request :get "/?page=3") app-4-pages :body)]
      (should= rsp "{:limit 5, :skip 10}"))
    (let [rsp (-> (request :get "/?page=4") app-4-pages :body)]
      (should= rsp "{:limit 5, :skip 15}")))

  (it "returns correct link headers"
    (let [rsp (-> (request :get "/") app-with-zero)]
      (should= (get-in rsp [:headers "Link"]) nil))
    (let [rsp (-> (request :get "/") app-1-page)]
      (should= (get-in rsp [:headers "Link"]) nil))
    (let [rsp (-> (request :get "/") app-2-pages)]
      (should= (get-in rsp [:headers "Link"]) "</?page=2>; rel=\"next\", </?page=2>; rel=\"last\""))
    (let [rsp (-> (request :get "/?page=2") app-2-pages)]
      (should= (get-in rsp [:headers "Link"]) "</?page=1>; rel=\"first\", </?page=1>; rel=\"prev\""))
    (let [rsp (-> (request :get "/?page=2") app-4-pages)]
      (should= (get-in rsp [:headers "Link"]) "</?page=1>; rel=\"first\", </?page=1>; rel=\"prev\", </?page=3>; rel=\"next\", </?page=4>; rel=\"last\""))
    (let [rsp (-> (request :get "/?page=3") app-4-pages)]
      (should= (get-in rsp [:headers "Link"]) "</?page=1>; rel=\"first\", </?page=2>; rel=\"prev\", </?page=4>; rel=\"next\", </?page=4>; rel=\"last\""))
    (let [rsp (-> (request :get "/?page=4") app-4-pages)]
      (should= (get-in rsp [:headers "Link"]) "</?page=1>; rel=\"first\", </?page=3>; rel=\"prev\""))))
