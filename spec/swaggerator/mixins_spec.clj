(ns swaggerator.mixins-spec
  (:use [swaggerator mixins core json]
        [ring.mock request]
        [speclj core]))

(def post-bin (atom nil))

(defresource test-coll
  :mixins [collection-resource]
  :link-to-item ::test-item
  :data-key :things
  :exists? (fn [ctx] {:things [{:name "a"} {:name "b"}]})
  :post! (fn [ctx] (->> ctx :request :non-query-params (reset! post-bin)))
  :count (constantly 2))

(defresource test-item
  :mixins [item-resource]
  :link-to-collection ::test-coll
  :url "/{name}"
  :data-key :thing
  :exists? (fn [ctx] {:thing {:name (-> ctx :request :route-params :name)}}))

(defcontroller test-ctrl
  :url "/test"
  :add-to-resources {:schema {:id "Test"
                              :properties {:name {:type "string"}}}}
  :resources [test-coll test-item])

(defroutes test-app
  :controllers [test-ctrl])

(describe "collection-resource"
  (it "outputs data using the presenter and handlers"
    (let [rsp (-> (request :get "/test")
                  (header "Accept" "application/hal+json")
                  test-app)]
      (should= {:_links {:item {:href "/test/{name}"
                                :templated true}}
                :_embedded {:things [{:_links {:self {:href "/test/a"}}
                                      :name "a"}
                                     {:_links {:self {:href "/test/b"}}
                                      :name "b"}]}}
               (unjsonify (:body rsp)))
      (should= "application/hal+json" (-> rsp :headers (get "Content-Type")))))

  (it "creates items"
    (let [rsp (-> (request :post "/test")
                  (header "Accept" "application/json")
                  (content-type "application/json")
                  (body "{\"name\":1}")
                  test-app)]
      (should= "/test/1" (-> rsp :headers (get "Location")))
      (should= {:name 1} @post-bin))))

(describe "item-resource"
  (it "outputs data using the presenter and handlers"
    (let [rsp (-> (request :get "/test/hello")
                  (header "Accept" "application/hal+json")
                  test-app)]
      (should= {:_links {:collection {:href "/test"}}
                :name "hello"}
               (unjsonify (:body rsp))))))
