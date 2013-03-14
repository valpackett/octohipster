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
  (:use [swaggerator json util]
        [swaggerator.link util]))

(def ^:dynamic *handled-content-types* (atom []))

(defn resp-with-links [ctx b]
  {:links (:links ctx)
   :link-templates (:link-templates ctx)
   :body b})

(defn wrap-handler-json
  "Wraps a handler with a JSON handler."
  [handler]
  (swap! *handled-content-types* conj "application/json")
  (fn [ctx]
    (case (-> ctx :representation :media-type)
      "application/json" (let [result (handler ctx)
                               k (:data-key result)]
                           (resp-with-links ctx (-> result k jsonify)))
      (handler ctx))))

(defn wrap-handler-edn
  "Wraps a handler with a EDN handler."
  [handler]
  (swap! *handled-content-types* conj "application/edn")
  (fn [ctx]
    (case (-> ctx :representation :media-type)
      "application/edn" (let [result (handler ctx)
                              k (:data-key result)]
                           (resp-with-links ctx (-> result k pr-str)))
      (handler ctx))))

(defn wrap-handler-msgpack
  "Wraps a handler with a MessagePack handler."
  [handler]
  (swap! *handled-content-types* conj "application/x-msgpack")
  (fn [ctx]
    (case (-> ctx :representation :media-type)
      "application/x-msgpack" (let [result (handler ctx)
                                    k (:data-key result)]
                                  (resp-with-links ctx (java.io.ByteArrayInputStream. (-> result k mp/pack))))
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
          (resp-with-links ctx (-> result k yaml/generate-string)))
      (handler ctx))))

(defn self-link [ctx dk x]
  (when-let [lm (-> ctx :resource :link-mapping)]
    (when-let [tpl (uri-template-for-rel ctx (dk (lm)))]
      (expand-uri-template tpl x))))

(defn add-self-hal-link [ctx dk x]
  (assoc x :_links {:self {:href (self-link ctx dk x)}}))

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
      "application/hal+json"
        (let [rsp (handler ctx)
              dk (:data-key rsp)
              result (dk rsp)
              result (if (map? result)
                       (hal-embedify ctx result)
                       {:_embedded {dk (map (partial hal-embedify ctx) (map (partial add-self-hal-link ctx dk) result))}})]
          {:body (jsonify (assoc result :_links (links-as-map rsp)))})
      (handler ctx))))

(defn add-self-cj-link [ctx dk x]
  (assoc x :href (self-link ctx dk x)))

(defn cj-wrap [ctx dk m]
  {:href (un-dotdot (str (or (-> ctx :request :context) "") (self-link ctx dk m)))
   :data (mapv (fn [[k v]] {:name k, :value v}) m)})

(defn wrap-handler-collection-json
  "Wraps handler with a Collection+JSON handler. Note: consumes links;
  requires wrapping the Ring handler with swaggerator.handlers/wrap-collection-json."
  [handler]
  (swap! *handled-content-types* conj "application/vnd.collection+json")
  (fn [ctx]
    (case (-> ctx :representation :media-type)
      "application/vnd.collection+json"
        (let [rsp (handler ctx)
              links (links-as-map rsp)
              dk (:data-key rsp)
              result (dk rsp)
              items (if (map? result)
                      [(-> (cj-wrap ctx dk result)
                           (assoc :links (-> links
                                             (dissoc "self")
                                             (dissoc "listing")))
                           (assoc :href (:href (get links "self"))))]
                      (map (partial cj-wrap ctx dk) result))
              coll {:version "1.0"
                    :href (if-let [up (get links "listing")]
                            (:href up)
                            (-> ctx :request :uri))
                    :links (if (map? result) [] links)
                    :items items}]
          {:body (jsonify {:collection coll})})
      (handler ctx))))

(defn wrap-handler-request-links [handler]
  (fn [ctx]
    (-> ctx
        (update-in [:links] concatv (-> ctx :request :links))
        (update-in [:link-templates] concatv (-> ctx :request :link-templates))
        handler)))

(defn wrap-handler-context-relative-links [handler]
  (fn [ctx]
    (let [uri-context (or (-> ctx :request :context) "")
          prepender (partial prepend-to-href uri-context)]
      (-> ctx
          (assoc :links (map prepender (:links ctx)))
          (assoc :link-templates (map prepender (:link-templates ctx)))
          handler))))

(defn wrap-default-handler
  "Wraps a handler with all the data format wrappers"
  [handler]
  (-> handler
      wrap-handler-edn
      wrap-handler-yaml
      wrap-handler-msgpack
      wrap-handler-hal-json
      wrap-handler-collection-json
      wrap-handler-json
      wrap-handler-context-relative-links
      wrap-handler-request-links))

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
