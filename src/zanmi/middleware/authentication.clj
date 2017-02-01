(ns zanmi.middleware.authentication
  (:require [zanmi.boundary.database :as db]
            [zanmi.boundary.signer :as signer]
            [zanmi.data.profile :as profile]
            [buddy.core.codecs :refer [bytes->str]]
            [buddy.core.codecs.base64 :as base64]
            [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; header parsing                                                           ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn base64-decode [string]
  (-> (base64/decode string)
      (bytes->str)))

(defn- parse-authorization [req scheme]
  (let [scheme-pattern (re-pattern (str "^" scheme " (.*)$"))]
    (some-> (:headers req)
            (get "authorization")
            (as-> header (re-find scheme-pattern header))
            (second))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; application authentication                                               ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parse-app-claims [req app-validater]
  (some->> (parse-authorization req "ZanmiAppToken")
           (signer/unsign app-validater)))

(defn wrap-app-claims [handler app-validater]
  (fn [req]
    (let [claims (parse-app-claims req app-validater)]
      (handler (assoc req :app-claims claims)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; user credential authentication                                           ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- parse-credentials [req db]
  (some-> (parse-authorization req "Basic")
          (base64-decode)
          (string/split #":" 2)
          (as-> creds (zipmap [:username :password] creds))))

(defn- authenticate-credentials [{:keys [username password] :as creds} db]
  (when (and username password)
    (some-> (db/fetch db username)
            (profile/authenticate password))))

(defn wrap-user-credentials [handler db]
  (fn [req]
    (let [creds (parse-credentials req db)
          authenticated (authenticate-credentials creds db)]
      (handler (assoc req :user-profile authenticated)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; user reset token authentication                                          ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- parse-reset-claims [req signer]
  (some->> (parse-authorization req "ZanmiResetToken")
           (signer/parse-reset-token signer)))

(defn wrap-reset-claims [handler signer]
  (fn [req]
    (let [claims (parse-reset-claims req signer)]
      (handler (assoc req :reset-claims claims)))))
