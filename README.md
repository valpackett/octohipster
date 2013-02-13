# swaggerator 
[![Build
Status](https://travis-ci.org/myfreeweb/swaggerator.png)](https://travis-ci.org/myfreeweb/swaggerator)

A REST API micro-framework for Clojure.

Includes:

- some Ring middleware (JSON params using [Cheshire](https://github.com/dakrone/cheshire), `*host*` binding, pagination, link header)
- syntactic sugar around [liberator](https://github.com/clojure-liberator/liberator) and [compojure](https://github.com/weavejester/compojure)
- composable handlers for liberator
- hypermedia support! [hal+json](http://stateless.co/hal_specification.html) and Link/Link-Template HTTP headers
- a [Swagger](https://github.com/wordnik/swagger-core/wiki) implementation (hence the name)
- JSON Schema validation using [this awesome library](https://github.com/fge/json-schema-validator) (the same schema is used for validation and documentation -- WINNING!)

## Usage

See src/example.clj; run with `lein run -m example`

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
