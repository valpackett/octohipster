(ns swaggerator.core-spec
  (:use [speclj core]
        [ring.mock request]
        [swaggerator core routes mixins json]))

(describe "defresource"
  (it "adds the id"
    (defresource aaa :a 1)
    (should= {:a 1 :id ::aaa} aaa)))

(describe "controller"
  (it "adds stuff to resources"
    (should= {:resources [{:a 1, :global 0}
                          {:a 2, :global 0}]}
             (controller :resources [{:a 1} {:a 2}]
                         :add-to-resources {:global 0})))

  (it "applies mixins to resources"
    (should= {:resources [{:a 1, :b 2, :c 2}]}
             (controller :resources [{:a 1, :mixins [#(assoc % :b (:c %))]}]
                         :add-to-resources {:c 2}))))

(describe "routes"
  (it "assembles the ring handler"
    (let [rsrc {:url "/{name}", :handle-ok (fn [ctx] (str "Hello " (-> ctx :request :route-params :name)))}
          cntr {:url "/hello", :resources [rsrc]}
          r (routes :controllers [cntr])]
      (should= "Hello me"
               (-> (request :get "/hello/me") r :body))))

  (it "replaces clinks"
    (defresource clwhat
      :url "/what")
    (defresource clhome
      :url "/home"
      :clinks {:wat ::clwhat}
      :handle-ok (fn [ctx] (last (first ((-> ctx :resource :clinks))))))
    (defcontroller clone
      :url "/one"
      :resources [clhome])
    (defcontroller cltwo
      :url "/two"
      :resources [clwhat])
    (defroutes clsite :controllers [clone cltwo])
    (should= "/two/what"
             (-> (request :get "/one/home") clsite :body)))

  (it "wraps with middleware"
    (defresource mwhello
      :url "/what"
      :middleware [(fn [handler] (fn [req] (handler (assoc req :from-middleware "hi"))))]
      :handle-ok (fn [ctx] (-> ctx :request :from-middleware)))
    (defroutes mwsite :controllers [{:url "", :resources [mwhello]}])
    (should= "hi"
             (-> (request :get "/what") mwsite :body)))

  (it "calls documenters"
    (defn dcdocumenter [options]
      (resource
        :url "/test-doc"
        :handle-ok (fn [ctx] (jsonify {:things (map (fn [r] {:url (:url r)}) (:resources options))}))))
    (defresource dchello
      :url "/what")
    (defroutes dcsite
      :controllers [{:url "", :resources [dchello]}]
      :documenters [dcdocumenter])
    (should= {:things [{:url "/what"}]}
             (-> (request :get "/test-doc") dcsite :body unjsonify))))
