;;;; Swaggerator REST API example
;;;; Has validation, pagination, rate limiting, Swagger documentation
(ns example
  (:use [compojure.core :only [defroutes]]
        [org.httpkit server]
        [swaggerator core pagination handlers]
        [ring.middleware ratelimit])
  (:import [org.bson.types ObjectId])
  (:require [monger.core :as mg]
            [monger.query :as mq]
            [monger.collection :as mc]))

;; Model (models are JSON Schemas + db functions)
;  in a real app, should be a namespace, i.e. used like contacts/schema instead of contacts-schema
(mg/connect!)
(mg/set-db! (mg/get-db "swaggerator-example"))
(def contacts-schema
  {:id "Contact"
   :properties {:name {:type "string"
                       :required true}
                :email {:type "string"}}
   :required [:name]})
(defn contacts-count []
  (mc/count "contacts"))
(defn contacts-all []
  (mq/with-collection "contacts"
    (mq/find {})
    (mq/skip *skip*)
    (mq/limit *limit*)))
(defn contacts-find-by-name [x]
  (mc/find-one-as-map "contacts" {:name x}))
(defn contacts-insert! [x]
  (mc/insert "contacts" (assoc x :_id (ObjectId.))))
(defn contacts-update! [x old]
  (mc/update "contacts" old x :multi false))
(defn contacts-delete! [x]
  (mc/remove "contacts" x))

;; Presenter (presenters are functions that transform data from the database before serialization
(defn contact-presenter [data]
  (-> data
      (dissoc :_id)))

;; Controller (controllers are collections of resources)
(def name-param
  {:name "name"
   :dataType "string"
   :paramType "path"
   :required "true"
   :description "The name of the contact"
   :allowMultiple false})

(def body-param
  {:dataType "Contact"
   :paramType "body"
   :required true
   :allowMultiple false})

(defcontroller contacts "/contacts"
  "Operations about contacts"
  (route "/" []
    (listing-resource "Operations with all contacts"
      :schema contacts-schema
      :presenter contact-presenter
      :child-url-template "/contacts/{name}"
      :children-key :contacts ; the key you set in :exists?
      :exists? (fn [ctx] {:contacts (contacts-all)})
      :count contacts-count
      :default-per-page 5
      :post! (fn [ctx] (-> ctx :request :params contacts-insert!))

      :doc {:get {:nickname "getContacts"
                  :responseClass "Contact"
                  :summary "Get all contacts"
                  :notes "Paginated"
                  :parameters []}
            :post {:nickname "createContact"
                   :summary "Create a contact"
                   :parameters [body-param]}}))

  (route "/:name" [name]
    (resource "Operations with individual contacts"
      :method-allowed? (request-method-in :get :head :put :delete)
      :schema contacts-schema
      :respond-with-entity? true
      :new? false
      :exists? (fn [ctx]
                 (when-let [e (contacts-find-by-name name)]
                   {:contact e
                    :links [{:href "/contacts" :rel "list"}]}))
      :handle-ok (default-entry-handler contact-presenter :contact)

      :can-put-to-missing? false
      :put! (fn [ctx]
              {:contact (-> ctx :request :params
                         (contacts-update! (:contact ctx))
                         :name contacts-find-by-name)})

      :delete! (fn [ctx]
                 (contacts-delete! (:contact ctx))
                 {:contact nil})

      :doc {:get {:nickname "getContact"
                  :responseClass "Contact"
                  :summary "Get the contact"
                  :parameters [name-param]}
            :put {:nickname "updateContact"
                  :responseClass "Contact"
                  :summary "Update the contact"
                  :parameters [name-param body-param]}
            :delete {:nickname "deleteContact"
                     :responseClass "void"
                     :summary "Delete the contact"
                     :parameters [name-param]}})))

;; Routes
(defroutes app-routes
  (nest contacts)
  (swagger-routes contacts))

(def app
  (-> app-routes
      (wrap-ratelimit {:limits [(ip-limit 1000)]})))

(defn -main [& args]
  (run-server app {:port 8008})
  (prn "Running on 8008"))
