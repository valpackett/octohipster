# swaggerator

A REST API micro-framework for Clojure.

Includes:

- some Ring middleware (JSON params using [Cheshire](https://github.com/dakrone/cheshire), `*host*` binding)
- syntactic sugar around [liberator](https://github.com/clojure-liberator/liberator) and [compojure](https://github.com/weavejester/compojure)
- a [Swagger](https://github.com/wordnik/swagger-core/wiki) implementation (hence the name)
- JSON Schema validation using [this awesome library](https://github.com/fge/json-schema-validator) (the same schema is used for validation and documentation -- WINNING!)

## Usage

Minimal example:

```clojure
(ns your.app
  (:use [compojure.core :only [defroutes]]
        [swaggerator core]))

(def thing-schema
  {:id "Thing"
   :properties {:name {:type "string"}}
   :required [:name]})

; Controllers are collections of resources
(defcontroller things "/things"
  "Operations about things"
  (route "/:name" [name]
    (resource "Operations with individual things"
      :method-allowed? (request-method-in :get :head)
      :schema thing-schema
      :doc {:get {:nickname "getThing"
                  :responseClass "Thing"
                  :summary "Get the thing"
                  :notes "Notes"
                  :parameters [{:name "name"
                                :description "Name"
                                :dataType "string"
                                :required true
                                :paramType "path"}]}}
      :handle-ok (fn [ctx] (str "Name: " name)))))

(defroutes app-routes
  (nest things)
  (swagger-routes things))

; Try /api-docs.json in this app
```

## License

Copyright 2013 Greg V <floatboth@me.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
