(ns swaggerator.handlers
  "Tools for creating handlers from presenters.

  Presenters are functions that process data from the database
  before sending it to the client. The simplest presenter is
  clojure.core/identity - ie. changing nothing.

  Handlers are functions that produce Ring responses from
  Liberator contexts. You pass handlers to resource parameters,
  usually :handle-ok.
  
  Handlers are composed like Ring middleware, but
  THEY ARE NOT RING MIDDLEWARE. They take a Liberator
  context as an argument, not a Ring request.
  When you create your own, follow the naming convention:
  wrap-handler-*, not wrap-*."
  (:require [clj-yaml.core :as yaml]
            [clj-msgpack.core :as mp])
  (:use [swaggerator json link util]))

(def ^:dynamic *handled-content-types* (atom []))

(defn wrap-handler-json
  "Wraps a handler with a JSON handler."
  [handler]
  (swap! *handled-content-types* conj "application/json")
  (fn [ctx]
    (case (-> ctx :representation :media-type)
      "application/json" (let [result (handler ctx)
                               k (:data-key result)]
                           (-> result k jsonify))
      (handler ctx))))

(defn wrap-handler-edn
  "Wraps a handler with a EDN handler."
  [handler]
  (swap! *handled-content-types* conj "application/edn")
  (fn [ctx]
    (case (-> ctx :representation :media-type)
      "application/edn" (let [result (handler ctx)
                              k (:data-key result)]
                           (-> result k pr-str))
      (handler ctx))))

(defn wrap-handler-msgpack
  "Wraps a handler with a MessagePack handler."
  [handler]
  (swap! *handled-content-types* conj "application/x-msgpack")
  (fn [ctx]
    (case (-> ctx :representation :media-type)
      "application/x-msgpack" (let [result (handler ctx)
                                    k (:data-key result)]
                                  (java.io.ByteArrayInputStream. (-> result k mp/pack)))
      (handler ctx))))

(defn wrap-handler-yaml
  "Wraps a handler with a YAML handler."
  [handler]
  (swap! *handled-content-types* conj "application/yaml")
  (swap! *handled-content-types* conj "application/x-yaml")
  (swap! *handled-content-types* conj "text/yaml")
  (swap! *handled-content-types* conj "text/x-yaml")
  (fn [ctx]
    (case (-> ctx :representation :media-type)
      ("application/yaml" "application/x-yaml"
       "text/yaml" "text/x-yaml")
        (let [result (handler ctx)
              k (:data-key result)]
          (-> result k yaml/generate-string))
      (handler ctx))))

; hal is implemented through a ring middleware
; because it needs to capture links that are not from liberator
(defn hal-links [rsp]
  (into {}
    (concatv
      (map (fn [x] [(:rel x) (-> x (dissoc :rel))]) (:links rsp))
      (map (fn [x] [(:rel x) (-> x (dissoc :rel) (assoc :templated true))]) (:link-templates rsp)))))

(defn add-self-hal-link [ctx dk x]
  (let [lm (or ((-> ctx :resource :link-mapping)) {})
        tpl (uri-template-for-rel ctx (dk lm))
        href (expand-uri-template tpl x)]
    (-> x
        (assoc :_links {:self {:href href}}))))

(defn add-nest-hal-link [ctx rel x y]
  (let [lm (or ((-> ctx :resource :embed-mapping)) {})
        tpl (uri-template-for-rel ctx rel)
        href (expand-uri-template tpl (merge x y))]
    (-> y
        (assoc :_links {:self {:href href}}))))

(defn hal-embedify [ctx x]
  (if-let [mapping (-> ctx :resource :embed-mapping)]
    (let [mapping (mapping)]
      (-> x
          (select-keys (filter #(not (mapping %)) (keys x)))
          (assoc :_embedded (into {} (map (fn [[k rel]] [k (mapv #(add-nest-hal-link ctx rel x %) (x k))]) mapping)))
          ))
    x))

(defn wrap-handler-hal-json
  "Wraps handler with a HAL+JSON handler. Note: consumes links;
  requires wrapping the Ring handler with swaggerator.handlers/wrap-hal-json."
  [handler]
  (swap! *handled-content-types* conj "application/hal+json")
  (fn [ctx]
    (case (-> ctx :representation :media-type)
      "application/hal+json" (let [result (-> ctx handler)
                                   dk (:data-key result)
                                   result (dk result)
                                   result (if (map? result)
                                            (hal-embedify ctx result)
                                            {:_embedded {dk (map (partial hal-embedify ctx) (map (partial add-self-hal-link ctx dk) result))}})]
                               {:_hal result})
      (handler ctx))))

(defn wrap-hal-json
  "Ring middleware for supporting the HAL+JSON handler wrapper."
  [handler]
  (fn [req]
    (let [rsp (handler req)]
      (if-let [hal (:_hal rsp)]
        (-> rsp
            (assoc :body
                   (-> hal
                       (assoc :_links (hal-links rsp))
                       jsonify))
            (dissoc :link-templates)
            (dissoc :links)
            (dissoc :_hal))
        rsp))))
; /hal

(defn wrap-handler-link
  "Wraps a handler with a function that passes :links and :link-templates
  to the response for consumption by swaggerator.handlers/wrap-hal-json,
  swaggerator.link/wrap-link-header or any other middleware."
  [handler]
  (fn [ctx]
    (let [result (handler ctx)]
      (if (map? result)
        (-> result
            (assoc :links (:links ctx))
            (assoc :link-templates (:link-templates ctx)))
        {:body result
         :links (:links ctx)
         :link-templates (:link-templates ctx)}))))

(defn wrap-default-handler
  "Wraps a handler with wrap-handler-hal-json, wrap-handler-json and wrap-handler-link."
  [handler]
  (-> handler
      wrap-handler-edn
      wrap-handler-yaml
      wrap-handler-msgpack
      wrap-handler-hal-json
      wrap-handler-json
      wrap-handler-link ; last!!
      ))

(defn list-handler
  "Makes a handler that maps a presenter over data that is retrieved
  from the Liberator context by given data key (by default :data)."
  ([presenter] (list-handler presenter :data))
  ([presenter k]
   (fn [ctx]
     (-> ctx
         (assoc :data-key k)
         (assoc k (mapv presenter (k ctx)))))))

(defn default-list-handler
  "list-handler wrapped in wrap-default-handler."
  ([presenter] (default-list-handler presenter :data))
  ([presenter k] (-> (list-handler presenter k)
                     wrap-default-handler)))

(defn entry-handler
  "Makes a handler that applies a presenter to data that is retrieved
  from the Liberator context by given data key (by default :data)."
  ([presenter] (entry-handler presenter :data))
  ([presenter k]
   (fn [ctx]
     (-> ctx
         (assoc :data-key k)
         (assoc k (presenter (k ctx)))))))

(defn default-entry-handler
  "entry-handler wrapped in wrap-default-handler."
  ([presenter] (default-entry-handler presenter :data))
  ([presenter k] (-> (entry-handler presenter k)
                     wrap-default-handler)))
