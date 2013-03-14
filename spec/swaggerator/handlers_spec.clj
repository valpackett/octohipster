(ns swaggerator.handlers-spec
  (:use [speclj core]
        [swaggerator handlers json]))

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

(describe "wrap-handler-msgpack"
  (it "outputs msgpack for msgpack requests"
    (let [h (-> identity wrap-handler-msgpack)
          ctx {:representation {:media-type "application/x-msgpack"}
               :data-key :things
               :things {:a 1}}]
      (should= (map int (slurp (:body (h ctx)))) [65533 65533 58 97 1])))
  (it "does not touch non-msgpack requests"
    (let [h (-> identity wrap-handler-msgpack)
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
