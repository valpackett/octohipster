Current [semantic](http://semver.org/) version:

```clojure
[swaggerator "0.1.2-SNAPSHOT"]
```

# swaggerator [![Build Status](https://travis-ci.org/myfreeweb/swaggerator.png?branch=master)](https://travis-ci.org/myfreeweb/swaggerator)

A REST framework for Clojure that allows you to easily build high performance web APIs that:

- support hypermedia ([HAL+JSON](http://stateless.co/hal_specification.html), [Collection+JSON](http://amundsen.com/media-types/collection/) and Link/Link-Template HTTP headers; you can use hypermedia tools like [Frenetic](http://dlindahl.github.com/frenetic/) to build clients for your API)
- have [Swagger](https://github.com/wordnik/swagger-core/wiki) documentation
- use [JSON Schema](http://json-schema.org) for validation *and* documentation
- have pagination
- are 100% [Ring](https://github.com/ring-clojure/ring); you can add [rate limiting](https://github.com/myfreeweb/ring-ratelimit), [authentication](https://github.com/cemerick/friend), [metrics](http://metrics-clojure.readthedocs.org/en/latest/ring.html) and more with just middleware.

Swaggerator is based on [Liberator](https://github.com/clojure-liberator/liberator) and [Compojure](https://github.com/weavejester/compojure).

## Usage

- For an example, see [src/example.clj](https://github.com/myfreeweb/swaggerator/blob/master/src/example.clj); run with `lein run -m example`
- [API Documentation](http://myfreeweb.github.com/swaggerator) is available

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
