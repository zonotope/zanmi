(ns zanmi.middleware.authentication
  (:require [zanmi.boundary.database :as db]
            [zanmi.boundary.signer :as signer]
            [zanmi.data.profile :as profile]
            [zanmi.util.codec :refer [base64-decode]]
            [buddy.auth.backends :as buddy-backend]
            [buddy.auth.middleware :as buddy-middleware]
            [clojure.string :as string]))

(defn wrap-authentication [app db]
  (let [authenticate (fn [req {:keys [username password] :as creds}]
                       (when (and username password)
                         (-> (db/fetch db username)
                             (profile/authenticate password))))
        auth-backend (buddy-backend/basic {:authfn authenticate})]
    (-> app
        (buddy-middleware/wrap-authentication auth-backend))))


(defn- wrap-parse-token-mw [app signer & {:keys [parse-fn param claim-key]}]
  (fn [req]
    (if-let [token (some-> req :params param)]
      (let [req-with-claim (if-let [claims (parse-fn signer token)]
                             (assoc req claim-key claims)
                             req)]
        (app req-with-claim))
      (app req))))

(defn wrap-parse-api-token [app validater]
  (wrap-parse-token-mw app validater
                       :parse-fn signer/unsign :param :app-token
                       :claim-key :app-claim))

(defn wrap-parse-reset-token [app signer]
  (wrap-parse-token-mw app signer
                       :parse-fn signer/parse-reset-token :param :reset-token
                       :claim-key :reset-claim))

(defn- parse-basic [headers db]
  (some-> headers
          (get "authorization")
          (as-> header (re-find #"^Basic (.*)$" header))
          (second)
          (base64-decode)
          (string/split #":" 2)
          (as-> creds (let [[username password] creds]
                        (-> (db/fetch db username)
                            (profile/authenticate password))))))

(defn wrap-parse-basic [app db]
  (fn [{:keys [headers] :as req}]
    (app (assoc req :user-profile (parse-basic headers db)))))
