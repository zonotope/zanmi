(ns zanmi.component.signer
  (:require [zanmi.component.signer.sha :refer [sha-signer]]))

(defn signer [config]
  (case (:algorithm config)
    :sha (sha-signer config)))
