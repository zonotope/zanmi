(ns zanmi.boundary.signer
  (:require [zanmi.util.time :as time]))

(defprotocol Signer
  "Sign data"
  (sign [signer data])
  (unsign [signer signed-data]))

(defn auth-token [signer profile]
  (let [now (time/now)
        exp (time/in-hours (:auth-expire-after signer))]
    (-> profile
        (select-keys [:id :username :modified])
        (assoc :action :authenticate, :iat now, :exp exp)
        (as-> data (sign signer data)))))

(defn reset-token [signer profile]
  (let [now (time/now)
        exp (time/in-hours (:reset-expire-after signer))]
    (-> profile
        (select-keys [:id :username])
        (assoc :action :reset, :iat now, :exp exp)
        (as-> data (sign signer data)))))
