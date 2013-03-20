(ns swaggerator.handlers.util
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
  (:use [swaggerator util]))

(defn resp-with-links [ctx b]
  {:links (:links ctx)
   :link-templates (:link-templates ctx)
   :body b})

(defn self-link [ctx rel x]
  (when-let [tpl (uri-template-for-rel ctx rel)]
    (expand-uri-template tpl x)))

(defn wrap-handler-add-clink [handler r templated]
  (fn [ctx]
    (-> ctx
        (update-in [(if templated :link-templates :links)] conj
                   {:rel (name r)
                    :href (r (apply hash-map (apply concat ((:clinks (:resource ctx))))))})
        handler)))
