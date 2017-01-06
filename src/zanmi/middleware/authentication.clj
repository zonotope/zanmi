(ns zanmi.middleware.authentication
  (:require [zanmi.boundary.database :as db]
            [zanmi.data.profile :as profile]
            [zanmi.view.profile-view :refer [render-error]]
            [buddy.auth.backends :as buddy-backend]
            [buddy.auth.middleware :as buddy-middleware]
            [ring.util.response :as response :refer [response]]))

(defn- error [e status]
  (-> (response (render-error e))
      (assoc :status status)))

(defn wrap-authentication [app db]
  (let [authenticate (fn [req {:keys [username password] :as creds}]
                       (when (and username password)
                         (-> (db/fetch db username)
                             (profile/authenticate password))))
        unauthorized (fn [req {:keys [reason] :as errdata}]
                       (case reason
                         :unauthenticated (error "bad username or password" 401)
                         :unauthorized (error "unauthorized" 409)))
        auth-backend (buddy-backend/basic {:authfn authenticate
                                           :unauthorized-handler unauthorized})]
    (-> app
        (buddy-middleware/wrap-authentication auth-backend)
        (buddy-middleware/wrap-authorization auth-backend))))
