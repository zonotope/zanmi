(ns zanmi.view.profile-view
  (:require [zanmi.boundary.signer :as signer]))

(defn render-auth-token [profile signer]
  {:auth-token (signer/auth-token signer profile)})

(defn render-message [message]
  {:message message})

(defn render-error [message]
  {:error message})
