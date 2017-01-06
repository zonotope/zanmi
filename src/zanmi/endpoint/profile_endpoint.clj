(ns zanmi.endpoint.profile-endpoint
  (:require [zanmi.boundary.database :as db]
            [zanmi.boundary.signer :as signer]
            [zanmi.data.profile :refer [create update]]
            [zanmi.view.profile-view :refer [render-error render-message
                                             render-auth-token
                                             render-reset-token]]
            [buddy.auth :refer [throw-unauthorized]]
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

(defn- reset [profile signer]
  (response (render-reset-token profile signer)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; request authentication / authorization                                   ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- authenticate [identity]
  (when-not identity
    (throw-unauthorized {:reason :unauthenticated}))
  identity)

(defn- authorize [identity username]
  (when-not (= (:username identity) username)
    (throw-unauthorized {:reason :unauthorized}))
  identity)

(defn- when-valid-reset-token [signer reset-token username validated-fn]
  (if-let [payload (signer/unsign signer reset-token)]
    (if (and (= username (:username payload))
             (= (:sub payload) "reset"))
      (validated-fn payload)
      (error "unauthorized" 409))
    (error "invalid reset token" 401)))

(defn- when-api-authenticated [validator request-token response-fn]
  (if-let [payload (signer/unsign validator request-token)]
    (response-fn payload)
    (error "invalid request token" 401)))

(defn- when-valid-reset-request [db username payload response-fn]
  (if (= username (:username payload))
    (when-let [profile (db/fetch db username)]
      (response-fn profile))
    (error "mismatched usernames" 409)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; actions                                                                  ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- create-profile [db schema signer attrs]
  (-> (create schema attrs)
      (as-> validated (db/save! db validated))
      (match {:ok new-profile} (created new-profile signer)
             {:error messages} (error messages 409))))

(defn- update-password [db schema signer profile new-password]
  (let [username (:username profile)]
    (-> (update schema profile new-password)
        (as-> validated (db/set! db username validated))
        (match {:ok new-profile} (ok new-profile signer)
               {:error messages} (error messages 400)))))

(defn- delete-profile [db {:keys [username] :as profile}]
  (when (db/delete! db username)
    (deleted username)))

(defn- show-auth-token [signer profile]
  (ok profile signer))

(defn- show-reset-token [signer profile]
  (reset profile signer))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; routes                                                                   ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn profile-routes [{:keys [api-validator db profile-schema signer]
                       :as endpoint}]
  (context route-prefix []
    (POST "/" [username password]
      (let [attrs {:username username :password password}]
        (create-profile db profile-schema signer attrs)))

    (context "/:username" [username :as {:keys [identity]}]
      (PUT "/" [reset-token new-password]
        (letfn [(update-pw [profile]
                  (update-password db profile-schema signer profile
                                   new-password))]
          (if reset-token
            (when-valid-reset-token signer reset-token username update-pw)
            (-> identity
                (authenticate)
                (authorize username)
                (update-pw)))))

      (DELETE "/" []
        (-> identity
            (authenticate)
            (authorize username)
            (as-> authorized (delete-profile db authorized))))

      (POST "/auth" []
        (-> identity
            (authenticate)
            (authorize username)
            (as-> authorized (show-auth-token signer authorized))))

      (POST "/reset" [request-token]
        (when-api-authenticated api-validator request-token
                                (fn [payload]
                                  (when-valid-reset-request db username payload
                                                            (fn [profile] (show-reset-token signer profile)))))))))
