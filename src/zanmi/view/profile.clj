(ns zanmi.view.profile
  (:require [buddy.sign.jwt :as jwt]))

(defn- sign [profile secret]
  (-> profile
      (select-keys [:id :username])
      (jwt/sign secret)))

(defn render-message [message]
  {:message message})

(defn render-token [profile secret]
  {:token (sign profile secret)})
