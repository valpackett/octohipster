(ns swaggerator.handlers-spec
  (:use [speclj core]
        [swaggerator handlers]))

(defn wrap-handler-test [handler]
  (fn [ctx] "hello"))

(describe "wrap-handler-json"
  (it "outputs json for json requests"
    (let [h (-> identity wrap-handler-json)
          ctx {:representation {:media-type "application/json"}
               :data-key :things
               :things {:a 1}}]
      (should= (h ctx) "{\"a\":1}")))
  (it "does not touch non-json requests"
    (let [h (-> identity wrap-handler-json)
          ctx {:representation {:media-type "text/plain"}}]
      (should= (h ctx) ctx))))

(describe "wrap-handler-edn"
  (it "outputs edn for edn requests"
    (let [h (-> identity wrap-handler-edn)
          ctx {:representation {:media-type "application/edn"}
               :data-key :things
               :things {:a 1}}]
      (should= (h ctx) "{:a 1}")))
  (it "does not touch non-edn requests"
    (let [h (-> identity wrap-handler-edn)
          ctx {:representation {:media-type "text/plain"}}]
      (should= (h ctx) ctx))))

(describe "wrap-handler-yaml"
  (it "outputs yaml for yaml requests"
    (let [h (-> identity wrap-handler-yaml)
          ctx {:representation {:media-type "application/yaml"}
               :data-key :things
               :things {:a 1}}]
      (should= (h ctx) "{a: 1}\n")))
  (it "does not touch non-yaml requests"
    (let [h (-> identity wrap-handler-yaml)
          ctx {:representation {:media-type "text/plain"}}]
      (should= (h ctx) ctx))))

(describe "wrap-handler-msgpack"
  (it "outputs msgpack for msgpack requests"
    (let [h (-> identity wrap-handler-msgpack)
          ctx {:representation {:media-type "application/x-msgpack"}
               :data-key :things
               :things {:a 1}}]
      (should= (map int (slurp (h ctx))) [65533 65533 58 97 1])))
  (it "does not touch non-msgpack requests"
    (let [h (-> identity wrap-handler-msgpack)
          ctx {:representation {:media-type "text/plain"}}]
      (should= (h ctx) ctx))))

(describe "wrap-handler-link"
  (it "passes :links and :link-templates to ring middleware"
    (let [links 1
          tpls 2
          h (-> identity wrap-handler-test wrap-handler-link)
          ctx {:links links :link-templates tpls}]
      (should= (h ctx) {:body "hello" :links links :link-templates tpls}))))

(describe "wrap-handler-hal-json"
  (it "consumes links and passes data to ring middleware for hal+json requests"
    (let [h (-> identity wrap-handler-hal-json wrap-handler-link)
          ctx {:representation {:media-type "application/hal+json"}
               :data-key :things
               :things {:a 1}}]
      (should= (h ctx) {:link-templates nil
                        :links nil
                        :_hal {:a 1}})))
  (it "creates an _embedded wrapper for non-map content and adds templated self links"
    (let [h (-> identity wrap-handler-hal-json)
          ctx {:representation {:media-type "application/hal+json"}
               ; liberator does this constantly thing
               :resource {:link-mapping (constantly {:things "things"})
                          :link-templates (constantly [{:rel "things" :href "/things/{a}"}])}
               :data-key :things
               :things [{:a 1}]}]
      (should= (h ctx)
               {:_hal {:_embedded {:things [{:a 1
                                             :_links {:self {:href "/things/1"}}}]}}})))
  (it "does not touch non-hal+json requests"
    (let [h (-> identity wrap-handler-hal-json)
          ctx {:representation {:media-type "application/json"}}]
      (should= (h ctx) ctx))))

(describe "entry-handler"
  (it "uses the presenter on the data"
    (let [h (entry-handler (partial + 1) :data)]
      (should= (h {:data 1}) {:data-key :data
                              :data 2}))))

(describe "list-handler"
  (it "maps the presenter over the data"
    (let [h (list-handler (partial + 1) :data)]
      (should= (h {:data [1 2]}) {:data-key :data
                                  :data [2 3]}))))
