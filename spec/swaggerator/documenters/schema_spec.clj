(ns swaggerator.documenters.schema-spec
  (:use [speclj core]
        [ring.mock request]
        [swaggerator core routes json]
        [swaggerator.documenters schema]))

(def contact-schema
  {:id "Contact"
   :type "object"
   :properties {:guid {:type "string"}}})

(defresource contact-collection)
(defresource contact-entry :url "/{id}")
(defcontroller contact-controller
  :url "/contacts"
  :add-to-resources {:schema contact-schema}
  :resources [contact-collection contact-entry])
(defroutes site
  :controllers [contact-controller]
  :documenters [schema-doc schema-root-doc])

(describe "schema-doc"
  (it "exposes schemas at /schema"
    (should= {:_links {:self {:href "/schema"}}
              :Contact contact-schema}
             (-> (request :get "/schema")
                 (header "Accept" "application/hal+json")
                 site :body unjsonify))))

(describe "schema-root-doc"
  (it "exposes schemas and controllers at /"
    (should= {:_links {:self {:href "/"}
                       :contacts {:href "/contacts"}}
              :_embedded {:schema {:_links {:self {:href "/schema"}}
                                   :Contact contact-schema}}}
             (-> (request :get "/")
                 (header "Accept" "application/hal+json")
                 site :body unjsonify))))
