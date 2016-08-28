(ns zanmi.util.validation
  (:require [clojure.spec :as spec]))

(let [error-fn-registry (atom {})]
  (defn register-error-fn! [sym message]
    (swap! error-fn-registry #(assoc % sym message)))

  (defn- error-fn [sym]
    (sym @error-fn-registry)))

(defn- error-message [sym data]
  ((error-fn sym) data))

(defmacro defvalidator [sym binding validation error-key message-body]
  `(let [message-fn# (fn ~binding ~message-body)]
     (register-error-fn! (quote ~sym) message-fn#)
     (defn ~sym ~binding ~validation)))

(defn explain-validators [spec data]
  (if-let [problems (::spec/problems (spec/explain-data spec data))]
    (map #(error-message (:pred %) (:val %)) problems)))
