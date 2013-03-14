(ns swaggerator.link
  (:use [swaggerator util])
  (:require [clojure.string :as string]))

(defn- make-link-header-field [[k v]]
  (format "%s=\"%s\"" (name k) v))

(defn- make-link-header-element [link]
  (let [fields (map make-link-header-field (dissoc link :href))]
    (format "<%s>%s"
            (:href link)
            (if (not (empty? fields))
              (->> fields
                   (interpose " ")
                   (apply str "; "))
              ""))))

(defn make-link-header
  "Compiles a collection of links into the RFC 5988 format.
  Links are required to be maps. The :href key going into the <> part.
  eg. {:href \"/hello\" :rel \"self\" :title \"Title\"}
      -> </hello>; rel=\"self\" title=\"Title\""
  [links]
  (if (empty? links) nil
    (->> links
         (map make-link-header-element)
         (interpose ", ")
         (apply str))))

(defn- wrap-link-header-1 [handler k h]
  (fn [req]
    (let [rsp (-> req
                  (assoc k (or (k req) []))
                  handler)]
      (-> rsp
          (assoc-in [:headers h] (-> rsp k make-link-header))
          (dissoc k)))))

(defn wrap-link-header
  "Ring middleware that compiles :links and :link-templates into
  Link and Link-Template headers using swaggerator.link/make-link-header."
  [handler]
  (-> handler
      (wrap-link-header-1 :links "Link")
      (wrap-link-header-1 :link-templates "Link-Template")))

(defn wrap-add-links-1 [handler links k]
  (fn [req]
    (let [rsp (handler req)]
      (assoc rsp k (concatv (or (k rsp) []) links)))))

(defn wrap-add-link-templates
  "Ring middleware that adds specified templates to :link-templates."
  [handler tpls] (wrap-add-links-1 handler tpls :link-templates))

(defn wrap-add-links
  "Ring middleware that adds specified links to :links."
  [handler links] (wrap-add-links-1 handler links :links))

(defn wrap-add-self-link
  "Ring middleware that adds a link to the requested URI as rel=self to :links."
  [handler]
  (fn [req]
    (let [rsp (handler req)]
      (assoc rsp :links
             (concatv (or (:links rsp) [])
                      [{:href (context-relative-uri req)
                        :rel "self"}])))))

(defn un-dotdot [x]
  (string/replace x #"/[^/]+/\.\." ""))

(defn prepend-to-href [uri-context l]
  (assoc l :href (un-dotdot (str uri-context (:href l)))))

(defn wrap-context-relative-links [handler]
  (fn [req]
    (let [uri-context (or (:context req) "")
          prepender (partial prepend-to-href uri-context)
          rsp (handler req)]
      (-> rsp
          (assoc :links (map prepender (:links rsp)))
          (assoc :link-templates (map prepender (:link-templates rsp)))))))
