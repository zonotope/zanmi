(ns zanmi.component.signer.ecdsa
  (:require [zanmi.boundary.signer :as signer]
            [buddy.core.keys :as keys]
            [buddy.sign.jwt :as jwt]))

(defn- alg-key [size]
  (if (= size 256)
    :es256
    :es512))

(defrecord EcdsaSigner [size public-key private-key]
  signer/Signer
  (sign [signer data]
    (let [alg (alg-key size)]
      (jwt/sign data private-key {:alg alg})))

  (unsign [signer signed-data]
    (let [alg (alg-key size)]
      (jwt/unsign signed-data public-key {:alg alg}))))

(defn ecdsa-signer [{:keys [size keypair] :as config}]
  (let [pubkey (keys/public-key (:public keypair))
        privkey (keys/private-key (:private keypair))]
    (->EcdsaSigner size pubkey privkey)))
