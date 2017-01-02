(ns zanmi.component.signer
  (:require [zanmi.component.signer.asymetric :refer [ecdsa-signer
                                                      rsa-pss-signer]]
            [zanmi.component.signer.sha :refer [sha-signer]]))

(defn signer [config]
  (case (:alg config)
    (:es256 :es512) (ecdsa-signer config)
    (:ps256 :ps512) (rsa-pss-signer config)
    (:hs256 :hs512) (sha-signer config)))
