(ns zanmi.boundary.signer)

(defprotocol Signer
  "Sign data"
  (sign [signer data])
  (unsign [signer signed-data]))
