(ns zanmi.endpoint.profile-endpoint
  (:require [zanmi.boundary.database :as db]
            [zanmi.boundary.signer :as signer]
            [zanmi.data.profile :refer [authenticate create update]]
            [zanmi.view.profile-view :refer [render-error render-message
                                             render-auth-token]]
            [clojure.core.match :refer [match]]
            [compojure.core :refer [context DELETE POST PUT]]
            [ring.util.response :as response :refer [response]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; url                                                                      ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private route-prefix "/profiles")

(defn- resource-url [{username :username :as profile}]
  (str route-prefix "/" username))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; responses                                                                ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- created [profile signer]
  (response/created (resource-url profile)
                    (render-auth-token profile signer)))

(defn- deleted [username]
  (let [deleted-message (format "profile for '%s' deleted" username)]
    (response (render-message deleted-message))))

(defn- error [e status]
  (-> (response (render-error e))
      (assoc :status status)))

(defn- ok [profile signer]
  (response (render-auth-token profile signer)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; request authentication / authorization                                   ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- when-user-authenticated [db username password response-fn]
  (if-let [profile (-> (db/fetch db username)
                       (authenticate password))]
    (response-fn profile)
    (error "bad username or password" 401)))

(defn- when-api-authenticated [validator request-token response-fn]
  (if-let [payload (signer/unsign validator request-token)]
    (response-fn payload)
    (error "invalid request token" 401)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; endpoint routes                                                          ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn profile-routes [{:keys [db profile-schema signer] :as endpoint}]
  (context route-prefix []
    (POST "/" [username password]
      (-> (create profile-schema {:username username :password password})
          (as-> validated (db/save! db validated))
          (match {:ok new-profile} (created new-profile signer)
                 {:error messages} (error messages 409))))

    (context "/:username" [username]
      (PUT "/" [password new-password]
        (when-user-authenticated db username password
                                 (fn [{:keys [username] :as profile}]
                                   (-> (update profile-schema profile new-password)
                                       (as-> validated (db/set! db username validated))
                                       (match {:ok new-profile} (ok new-profile signer)
                                              {:error messages} (error messages 400))))))

      (DELETE "/" [password]
        (when-user-authenticated db username password
                                 (fn [{:keys [username] :as profile}]
                                   (when (db/delete! db username)
                                     (deleted username)))))

      (POST "/auth" [password]
        (when-user-authenticated db username password
                                 (fn [profile] (ok profile signer)))))))
