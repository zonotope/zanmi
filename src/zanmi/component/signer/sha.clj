(ns zanmi.component.signer.sha
  (:require [zanmi.boundary.signer :as signer]
            [buddy.sign.jwt :as jwt]))

(defn- alg-key [size]
  (if (= size 256)
    :hs256
    :hs512))

(defrecord ShaSigner [size secret]
  signer/Signer
  (sign [signer data]
    (let [alg (alg-key size)]
      (jwt/sign data secret {:alg alg})))

  (unsign [signer signed-data]
    (let [alg (alg-key size)]
      (jwt/unsign signed-data secret {:alg alg}))))

(defn sha-signer [{:keys [size secret] :as config}]
  (->ShaSigner size secret))
