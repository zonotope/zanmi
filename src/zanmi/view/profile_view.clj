(ns zanmi.view.profile-view
  (:require [buddy.sign.jwt :as jwt]))

(defn- sign [profile secret]
  (-> profile
      (select-keys [:id :username :modified])
      (jwt/sign secret)))

(defn render-token [profile secret]
  {:token (sign profile secret)})

(defn render-message [message]
  {:message message})

(defn render-error [message]
  {:error message})
