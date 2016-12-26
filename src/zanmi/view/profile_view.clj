(ns zanmi.view.profile-view
  (:require [zanmi.boundary.signer :as signer]))

(defn- token [profile signer]
  (-> profile
      (select-keys [:id :username :modified])
      (as-> data (signer/sign signer data))))

(defn render-token [profile signer]
  {:token (token profile signer)})

(defn render-message [message]
  {:message message})

(defn render-error [message]
  {:error message})
