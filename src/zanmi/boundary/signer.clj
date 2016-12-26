(ns zanmi.boundary.signer)

(defprotocol Signer
  "Sign data"
  (sign [signer data])
  (unsign [signer signed-data]))

(defn auth-token [signer profile]
  (-> profile
      (select-keys [:id :username :modified])
      (assoc :action :authenticate)
      (as-> data (sign signer data))))

(defn reset-token [signer profile]
  (-> profile
      (select-keys [:id :username])
      (assoc :action :reset)
      (as-> data (sign signer data))))
