(ns octohipster.handlers.util
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
  (:use [octohipster util host]))

(defn resp-common [ctx]
  {:data-key (:data-key ctx)})

(defn resp-linked [ctx]
  (-> ctx resp-common
      (assoc :links (:links ctx))
      (assoc :link-templates (:link-templates ctx))))

(defn self-link [ctx rel x]
  (when-let [tpl (uri-template-for-rel ctx rel)]
    (expand-uri-template tpl x)))

(defn templated? [[k v]]
  (.contains v "{"))

(defn expand-clinks [x]
  (map (fn [[k v]] {:rel (name k), :href (str *context* v)}) x))

(def process-clinks
  (memoize
    (fn [clinks]
      (let [clinks (apply hash-map (apply concat clinks))]
        [(expand-clinks (filter (complement templated?) clinks))
         (expand-clinks (filter templated? clinks))]))))

(defn wrap-handler-add-clinks [handler]
  (fn [ctx]
    (let [clinks (process-clinks ((:clinks (:resource ctx))))]
      (-> ctx
          (update-in [:links] concat (first clinks))
          (update-in [:link-templates] concat (last clinks))
          handler))))

(defn handler [ctypes fun]
  (let [ctypes-set (set ctypes)]
    (fn [hdlr]
      (fn [ctx]
        (if (contains? ctypes-set (-> ctx :representation :media-type))
          (fun hdlr ctx)
          (hdlr ctx))))))

(defmacro defhandler "Defines a handler." [n doc ctypes fun]
  `(def ~n ~doc (with-meta (handler ~ctypes ~fun) {:ctypes ~ctypes})))

(defn data-from-result [result]
  ((:data-key result) result))

(defn make-handler-fn [f]
  (fn [hdlr ctx]
    (-> ctx resp-linked
        (assoc :encoder f)
        (assoc :body (-> ctx hdlr data-from-result)))))

(defn wrap-apply-encoder [handler]
  ; used as ring middleware in apps, as handler wrapper in unit tests
  (fn [req]
    (let [rsp (handler req)]
      (if-let [enc (:encoder rsp)]
        (assoc rsp :body ((:encoder rsp) (:body rsp)))
        rsp))))
