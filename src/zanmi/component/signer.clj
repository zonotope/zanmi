(ns zanmi.component.signer
  (:require [zanmi.component.signer.asymetric :refer [ecdsa-signer
                                                      rsa-pss-signer]]
            [zanmi.component.signer.sha :refer [sha-signer]]))

(defn signer [config]
  (case (:algorithm config)
    :ecdsa (ecdsa-signer config)
    :rsa-pss (rsa-pss-signer config)
    :sha (sha-signer config)))
