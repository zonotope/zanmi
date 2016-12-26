(ns zanmi.component.signer.sha
  (:require [zanmi.boundary.signer :as signer]
            [buddy.sign.jwt :as jwt]))

(defrecord ShaSigner [alg secret]
  signer/Signer
  (sign [signer data]
    (jwt/sign data secret {:alg alg}))

  (unsign [signer data]
    (jwt/unsign data secret {:alg alg})))

(defn sha-signer [{:keys [size secret] :as config}]
  (let [alg (if (= size 256) :hs256 :hs512)]
    (->ShaSigner alg secret)))
