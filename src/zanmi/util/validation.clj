(ns zanmi.util.validation
  (:require [bouncer.core :as bouncer]))

(defn- with-fn-messages [{:keys [message metadata path value] :as error}]
  (let [message-fn (:message-fn metadata)]
    (if (and (fn? message-fn) (not message))
      (message-fn path value)
      (bouncer/with-default-messages error))))

(defn validate [data schema]
  (bouncer/validate with-fn-messages data schema))
