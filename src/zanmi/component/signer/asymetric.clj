(ns zanmi.component.signer.asymetric
  (:require [zanmi.boundary.signer :as signer]
            [buddy.core.keys :as keys]
            [buddy.sign.jwt :as jwt]))

(defn- alg-key [alg size]
  (keyword (str (name alg) size)))

(defrecord AsymetricSigner [alg size public-key private-key]
  signer/Signer
  (sign [signer data]
    (let [algorithm (alg-key alg size)]
      (jwt/sign data private-key {:alg algorithm})))

  (unsign [signer signed-data]
    (let [algorithm (alg-key alg size)]
      (jwt/unsign signed-data public-key {:alg algorithm}))))

(defn- asymetric-signer [algorithm {:keys [keypair] :as config}]
  (let [pubkey (keys/public-key (:public keypair))
        privkey (keys/private-key (:private keypair))]
    (-> config
        (dissoc :keypair)
        (assoc :alg algorithm, :public-key pubkey, :private-key privkey)
        (map->AsymetricSigner))))

(defn ecdsa-signer [config]
  (asymetric-signer :es config))

(defn rsa-pss-signer [config]
  (asymetric-signer :ps config))
