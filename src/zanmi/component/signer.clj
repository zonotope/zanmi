(ns zanmi.component.signer
  (:require [zanmi.component.signer.rsa-pss :refer [rsa-pss-signer]]
            [zanmi.component.signer.sha :refer [sha-signer]]))

(defn signer [config]
  (case (:algorithm config)
    :rsa-pss (rsa-pss-signer config)
    :sha (sha-signer config)))
