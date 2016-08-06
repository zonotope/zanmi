(ns zanmi.view.profile
  (:require [buddy.sign.jwt :as jwt]))

(defn- sign [profile secret]
  (-> profile
      (select-keys [:id :username])
      (jwt/sign secret)))

(defn render-message [message]
  {:message message})

(defn render-error [message]
  {:error message})

(defn render-token [profile secret]
  {:token (sign profile secret)})

(def auth-error (render-error "bad username or password"))

(def create-error (render-error "username is already taken"))

(defn deleted-message [username]
  (render-message (str username " deleted")))
