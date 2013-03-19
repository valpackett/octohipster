(ns swaggerator.handlers-spec
  (:use [speclj core]
        [swaggerator json]
        [swaggerator.handlers core json edn yaml hal cj]))

(defn wrap-handler-test [handler]
  (fn [ctx] "hello"))

(describe "wrap-handler-json"
  (it "outputs json for json requests"
    (let [h (-> identity wrap-handler-json)
          ctx {:representation {:media-type "application/json"}
               :data-key :things
               :things {:a 1}}]
      (should= (:body (h ctx)) "{\"a\":1}")))

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
      (should= (:body (h ctx)) "{:a 1}")))

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
      (should= (:body (h ctx)) "{a: 1}\n")))

  (it "does not touch non-yaml requests"
    (let [h (-> identity wrap-handler-yaml)
          ctx {:representation {:media-type "text/plain"}}]
      (should= (h ctx) ctx))))

(describe "wrap-handler-hal-json"
  (it "consumes links for hal+json requests"
    (let [h (-> identity wrap-handler-hal-json)
          ctx {:representation {:media-type "application/hal+json"}
               :data-key :things
               :things {:a 1}}]
      (should= (unjsonify (:body (h ctx)))
               {:_links {}
                :a 1})))

  (it "creates an _embedded wrapper for non-map content and adds templated self links"
    (let [h (-> identity wrap-handler-hal-json)
          ctx {:representation {:media-type "application/hal+json"}
               ; liberator does this constantly thing
               :resource {:link-mapping (constantly {:things "things"})
                          :link-templates (constantly [{:rel "things" :href "/things/{a}"}])}
               :data-key :things
               :things [{:a 1}]}]
      (should= (unjsonify (:body (h ctx)))
               {:_links {}
                :_embedded {:things [{:a 1
                                      :_links {:self {:href "/things/1"}}}]}})))

  (it "creates an _embedded wrapper for embed-mapping"
    (let [h (-> identity wrap-handler-hal-json)
          ctx {:representation {:media-type "application/hal+json"}
               :resource {:embed-mapping (constantly {:things "thing"})
                          :link-templates (constantly [{:rel "thing" :href "/yo/{a}/things/{b}"}])}
               :data-key :yo
               :yo {:a 1 :things [{:b 2}]}}]
      (should= (unjsonify (:body (h ctx)))
               {:_links {}
                :_embedded {:things [{:b 2
                                      :_links {:self {:href "/yo/1/things/2"}}}]}
                :a 1})))

  (it "does not touch non-hal+json requests"
    (let [h (-> identity wrap-handler-hal-json)
          ctx {:representation {:media-type "application/json"}}]
      (should= (h ctx) ctx))))

(describe "wrap-handler-collection-json"
  (it "consumes links for collection+json requests, to the item if data is a map"
    (let [h (-> identity wrap-handler-collection-json)
          ctx {:representation {:media-type "application/vnd.collection+json"}
               :links [{:rel "test", :href "/hello"}]
               :data-key :things
               :things {:a 1}}
          ctx2 (assoc ctx :things [{:a 1}])]
      (should= (-> ctx h :body unjsonify :collection :items first :links)
               [{:rel "test", :href "/hello"}])
      (should= (-> ctx2 h :body unjsonify :collection :links)
               [{:rel "test", :href "/hello"}])))

  (it "converts nested maps into collection+json format"
    (let [h (-> identity wrap-handler-collection-json)
          ctx {:representation {:media-type "application/vnd.collection+json"}
               :data-key :things
               :things {:hello {:world 1}}}]
      (should= (-> ctx h :body unjsonify :collection :items first :data)
               [{:name "hello"
                 :value [{:name "world"
                          :value 1}]}])))

  (it "does not touch non-collection+json requests"
    (let [h (-> identity wrap-handler-collection-json)
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
