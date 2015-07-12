Current [semantic](http://semver.org/) version:

```clojure
[octohipster "0.2.1-SNAPSHOT"]
```

# octohipster [![Build Status](https://travis-ci.org/myfreeweb/octohipster.png?branch=master)](https://travis-ci.org/myfreeweb/octohipster) [![unlicense](https://img.shields.io/badge/un-license-green.svg?style=flat)](http://unlicense.org)

Octohipster is

- a REST library/toolkit/microframework for Clojure
- that allows you to build HTTP APIs
- in a declarative [Webmachine](https://github.com/basho/webmachine/wiki/Overview)-like style, using [Liberator](https://github.com/clojure-liberator/liberator)
- powered by [Ring](https://github.com/ring-clojure/ring); you can add [rate limiting](https://github.com/myfreeweb/ring-ratelimit), [authentication](https://github.com/cemerick/friend), [metrics](http://metrics-clojure.readthedocs.org/en/latest/ring.html), [URL rewriting](https://github.com/ebaxt/ring-rewrite) and more with just middleware

It allows you to make APIs that

- support hypermedia ([HAL+JSON](http://stateless.co/hal_specification.html), [Collection+JSON](http://amundsen.com/media-types/collection/) and Link/Link-Template HTTP headers; works with [Frenetic](http://dlindahl.github.com/frenetic/))
- support multiple output formats (JSON, EDN, YAML and any custom format)
- have [Swagger](https://github.com/wordnik/swagger-core/wiki) documentation 
- use [JSON Schema](http://json-schema.org) for validation *and* documentation
- have pagination

## Concepts

- a **resource** is a single endpoint that accepts requests and returns responses
- a **group** is a collection of resources with a single URL prefix (eg. a group /things contains resources /things/ and /things/{id}) and zero or more shared properties (usually the schema)
- a **documenter** is a function that returns a resource which documents regular resources (Swagger, HAL root, etc)
- a **mixin** is a function that is applied to multiple resources to give them shared behavior (eg. collection or entry behavior)
- a **response handler** is a function that is used to encode response data to a particular content-type (JSON, EDN, YAML, etc.)
- a **params handler** is a function that is used to decode incoming data from a particular content-type (JSON, EDN, YAML, etc.)

## Usage

```clojure
(ns example
  (:use [octohipster core routes mixins pagination]
        [octohipster.documenters swagger schema]
        org.httpkit.server)
  (:import org.bson.types.ObjectId)
  (:require [monger.core :as mg]
            [monger.query :as mq]
            [monger.collection :as mc]
            monger.json))

(mg/connect!)
(mg/set-db! (mg/get-db "octohipster-example"))

;;;; The "model"
;;;;  tip: make it a separate namespace, eg. app.models.contact
(def contact-schema
  {:id "Contact"
   :type "object"
   :properties {:name {:type "string"}
                :phone {:type "integer"}}
   :required [:name]})

(defn contacts-count [] (mc/count "contacts"))
(defn contacts-all []
  (mq/with-collection "contacts"
    (mq/find {})
    (mq/skip *skip*)
    (mq/limit *limit*)))
(defn contacts-find-by-id [x] (mc/find-map-by-id "contacts" (ObjectId. x)))
(defn contacts-insert! [x]
  (let [id (ObjectId.)]
    (mc/insert "contacts" (assoc x :_id id))
    (mc/find-map-by-id "contacts" id)))
(defn contacts-update! [x old] (mc/update "contacts" old x :multi false))
(defn contacts-delete! [x] (mc/remove "contacts" x))

;;;; The resources
;; with shared pieces of documentation
(def name-param
  {:name "name", :dataType "string", :paramType "path", :required "true", :description "The name of the contact", :allowMultiple false})

(def body-param
  {:dataType "Contact", :paramType "body", :required true, :allowMultiple false})

(defresource contact-collection
  :desc "Operations with multiple contacts"
  :mixins [collection-resource]
  :clinks {:item ::contact-item}
  :data-key :contacts
  :exists? (fn [ctx] {:contacts (contacts-all)})
  :post! (fn [ctx] {:item (-> ctx :request :non-query-params contacts-insert!)})
  :count (fn [req] (contacts-count))
  :doc {:get {:nickname "getContacts", :summary "Get all contacts"}
        :post {:nickname "createContact", :summary "Create a contact"}})

(defresource contact-item
  :desc "Operations with individual contacts"
  :url "/{_id}"
  :mixins [item-resource]
  :clinks {:collection ::contact-collection}
  :data-key :contact
  :exists? (fn [ctx]
             (if-let [doc (-> ctx :request :route-params :_id contacts-find-by-id)]
               {:contact doc}))
  :put! (fn [ctx]
          (-> ctx :request :non-query-params (contacts-update! (:contact ctx)))
          {:contact (-> ctx :request :route-params :_id contacts-find-by-id)})
  :delete! (fn [ctx]
             (-> ctx :contact contacts-delete!)
             {:contact nil})
  :doc {:get {:nickname "getContact", :summary "Get a contact", :parameters [name-param]}
        :put {:nickname "updateContact", :summary "Overwrite a contact", :parameters [name-param body-param]}
        :delete {:nickname "deleteContact", :summary "Delete a contact", :parameters [name-param]}})

;;;; The group
(defgroup contact-group
  :url "/contacts"
  :add-to-resources {:schema contact-schema}  ; instead of typing the same for all resources in the group
  :resources [contact-collection contact-item])

;;;; The handler
(defroutes site
  :groups [contact-group]
  :documenters [schema-doc schema-root-doc swagger-doc swagger-root-doc])

(defn -main [] (run-server site {:port 8080}))
```

Also, [API Documentation](http://myfreeweb.github.com/octohipster) is available.

## Contributing

By participating in this project you agree to follow the [Contributor Code of Conduct](http://contributor-covenant.org/version/1/1/0/).

Please take over the whole project!  
I don't use Clojure a lot nowadays.  
Talk to me: <greg@unrelenting.technology>.

## License

This is free and unencumbered software released into the public domain.  
For more information, please refer to the `UNLICENSE` file or [unlicense.org](http://unlicense.org).
