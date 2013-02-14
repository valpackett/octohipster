Current [semantic](http://semver.org/) version:

```clojure
[swaggerator "0.1.0-SNAPSHOT"]
```

# swaggerator [![Build Status](https://travis-ci.org/myfreeweb/swaggerator.png)](https://travis-ci.org/myfreeweb/swaggerator)

A REST framework for Clojure that allows you to easily build high performance web APIs that:

- support hypermedia ([HAL+JSON](http://stateless.co/hal_specification.html) and Link/Link-Template HTTP headers; you can use hypermedia tools like [Frenetic](http://dlindahl.github.com/frenetic/) to build clients for your API)
- have [Swagger](https://github.com/wordnik/swagger-core/wiki) documentation
- use JSON Schema for validation *and* documentation
- have pagination
- are 100% [Ring](https://github.com/ring-clojure/ring); you can add [rate limiting](https://github.com/myfreeweb/ring-ratelimit) and [authentication](https://github.com/cemerick/friend) and [metrics](http://metrics-clojure.readthedocs.org/en/latest/ring.html) with just middleware

## Usage

For an example, see src/example.clj; run with `lein run -m example`.

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
