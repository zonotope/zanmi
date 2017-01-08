(ns zanmi.middleware.authentication
  (:require [zanmi.boundary.database :as db]
            [zanmi.boundary.signer :as signer]
            [zanmi.data.profile :as profile]
            [zanmi.util.response :refer [error]]
            [buddy.auth.accessrules :as access]
            [buddy.auth.backends :as buddy-backend]
            [buddy.auth.middleware :as buddy-middleware]))

(defn- wrap-parse-reset-token [app signer]
  (fn [req]
    (if-let [token (some-> req :params :reset)]
      (if-let [claims (signer/parse-reset-token signer token)]
        (app (assoc req :reset-claim claims))
        (error "invalid reset token" 401))
      (app req))))

(defn- wrap-parse-api-token [app validater]
  (fn [req]
    (if-let [token (some-> req :params :app-token)]
      (if-let [claims (signer/unsign validater token)]
        (app (assoc req :app-claim claims))
        (error "invalid app token" 401))
      (app req))))

(defn wrap-authentication [app db signer validater]
  (let [authenticate (fn [req {:keys [username password] :as creds}]
                       (when (and username password)
                         (-> (db/fetch db username)
                             (profile/authenticate password))))
        auth-backend (buddy-backend/basic {:authfn authenticate})]
    (-> app
        (buddy-middleware/wrap-authentication auth-backend)
        (wrap-parse-reset-token signer)
        (wrap-parse-api-token validater))))
