(ns zanmi.component.signer.asymetric
  (:require [zanmi.boundary.signer :as signer]
            [buddy.core.keys :as keys]
            [buddy.sign.jwt :as jwt]))

(defrecord AsymetricSigner [alg public-key private-key]
  signer/Signer
  (sign [signer data]
    (jwt/sign data private-key {:alg alg}))

  (unsign [signer signed-data]
    (jwt/unsign signed-data public-key {:alg alg})))

(defn- asymetric-signer [{:keys [alg keypair] :as config}]
  (let [pubkey (keys/public-key (:public keypair))
        privkey (keys/private-key (:private keypair))]
    (-> config
        (dissoc :keypair)
        (assoc :alg alg, :public-key pubkey, :private-key privkey)
        (map->AsymetricSigner))))

(defn ecdsa-signer [config]
  (asymetric-signer config))

(defn rsa-pss-signer [config]
  (asymetric-signer config))
