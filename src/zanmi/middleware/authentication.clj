(ns zanmi.middleware.authentication
  (:require [zanmi.boundary.database :as db]
            [zanmi.data.profile :as profile]
            [buddy.auth.backends :as buddy-backend]
            [buddy.auth.middleware :as buddy-middleware]))

(defn wrap-authentication [app db]
  (let [authenticate (fn [req {:keys [username password] :as creds}]
                       (when (and username password)
                         (-> (db/fetch db username)
                             (profile/authenticate password))))
        auth-backend (buddy-backend/basic {:authfn authenticate})]
    (buddy-middleware/wrap-authentication app auth-backend)))
