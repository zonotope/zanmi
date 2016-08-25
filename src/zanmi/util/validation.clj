(ns zanmi.util.validation
  (:require [clojure.spec :as spec]))

(let [error-message-registry (atom {})]
  (defn- register-error-message! [sym message]
    (swap! error-message-registry #(assoc % sym message)))

  (defn- error-message [sym]
    (sym @error-message-registry)))

(defmacro defvalidator [sym binding validation message]
  (register-error-message! sym message)
  `(defn ~sym ~binding ~validation))

(defn explain-validators [spec data]
  (if-let [problems (::spec/problems (spec/explain-data spec data))]
    (map #(error-message (:pred %)) problems)))
