(ns octohipster.problems
  (:require [clojure.string :as string])
  (:use [octohipster host]))

(defn wrap-expand-problems [handler problems]
  (fn [req]
    (let [rsp (handler req)]
      (if-let [prob (:problem rsp)]
        (let [{:keys [status title]} (prob problems)]
          (-> rsp
              (assoc :status status)
              (assoc :problem-type true)
              (assoc :body (-> rsp :body
                               (assoc :title title)
                               (assoc :problemType (str *host* *context* "/problems/" (name prob)))))
              (dissoc :problem)))
        rsp))))

(defn problemify-ctype [x]
  (string/replace x #"/([^\+]+\+)?" "/api-problem+"))

(defn wrap-expand-problem-ctype [handler]
  (fn [req]
    (let [rsp (handler req)]
      (if (:problem-type rsp)
        (update-in rsp [:headers "content-type"] problemify-ctype)
        rsp))))
