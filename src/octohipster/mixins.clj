(ns octohipster.mixins
  (:require [liberator.core :as lib])
  (:use [octohipster pagination validator util]
        [octohipster.handlers core json edn yaml hal cj util]
        [octohipster.link util]
        [swiss-arrows core]))

(defn validated-resource [r]
  (update-in r [:middleware] conj #(wrap-json-schema-validator % (:schema r))))

(defn handled-resource
  ([r] (handled-resource r item-handler))
  ([r handler]
   (let [r (merge {:handlers [wrap-handler-json wrap-handler-edn wrap-handler-yaml
                              wrap-handler-hal-json wrap-handler-collection-json]
                   :data-key :data
                   :presenter identity}
                  r)
         h (-<> (handler (:presenter r) (:data-key r))
                (reduce #(%2 %1) <> (:handlers r))
                (wrap-handler-add-clinks)
                wrap-default-handler)]
     (-> r
         (assoc :handle-ok h)
         (assoc :available-media-types
                (mapcat (comp :ctypes meta) (:handlers r)))))))

(defn item-resource
  "Mixin that includes all boilerplate for working with single items:
   - validation (using JSON schema in :schema for PUT requests)
   - format handling
   - linking to the item's collection"
  [r]
  (let [r (merge {:method-allowed? (lib/request-method-in :get :put :delete)
                  :collection-key :collection
                  :respond-with-entity? true
                  :new? false
                  :can-put-to-missing? false}
                 r)]
    (-> r
        validated-resource
        (handled-resource item-handler))))

(defn collection-resource
  "Mixin that includes all boilerplate for working with collections of items:
   - validation (using JSON schema in :schema for POST requests)
   - format handling
   - linking to the individual items
   - pagination"
  [r]
  (let [r (merge {:method-allowed? (lib/request-method-in :get :post)
                  :data-key :data
                  :item-key :item
                  :post-redirect? true
                  :is-multiple? true
                  :default-per-page 25}
                 r)]
    (-> r
        (assoc :see-other (params-rel (:item-key r)))
        (update-in [:middleware] conj
                   #(wrap-pagination % {:counter (:count r)
                                        :default-per-page (:default-per-page r)}))
        validated-resource
        (handled-resource collection-handler))))
