(ns zanmi.boundary.signer
  (:require [zanmi.util.time :as time]))

(defprotocol Signer
  "Sign data"
  (sign [signer data])
  (unsign [signer signed-data]))

(defn auth-token [signer profile]
  (let [now (time/now)
        exp (time/in-hours (:auth-exp signer))]
    (-> profile
        (select-keys [:id :username :modified])
        (assoc :sub "authenticate", :iat now, :exp exp)
        (as-> data (sign signer data)))))

(defn reset-token [signer profile]
  (let [now (time/now)
        exp (time/in-hours (:reset-exp signer))]
    (-> profile
        (select-keys [:id :username])
        (assoc :sub "reset", :iat now, :exp exp)
        (as-> data (sign signer data)))))

(defn parse-reset-token [signer token]
  (let [payload (unsign signer token)]
    (when (= (:sub payload) "reset")
      payload)))
