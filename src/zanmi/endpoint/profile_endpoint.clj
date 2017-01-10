(ns zanmi.endpoint.profile-endpoint
  (:require [zanmi.boundary.database :as db]
            [zanmi.boundary.signer :as signer]
            [zanmi.data.profile :refer [create update]]
            [zanmi.view.profile-view :refer [render-error render-message
                                             render-auth-token
                                             render-reset-token]]
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
;; request authorization                                                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- authorize-profile [profile username action]
  (if profile
    (if (= (:username profile) username)
      (action profile)
      (error "unauthorized" 409))
    (error "bad username or password" 401)))

(defn- authorize-reset [reset-claim username action]
  (if (= (:username reset-claim) username)
    (action reset-claim)
    (error "unauthorized" 409)))

(defn- authorize-app [app-claim username action]
  (if (= (:username app-claim) username)
    (action app-claim)
    (error "unauthorized" 409)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; routes                                                                   ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn profile-routes [{:keys [db profile-schema signer] :as endpoint}]
  (context route-prefix []
    (POST "/" [username password]
      (let [attrs {:username username :password password}]
        (create-profile db profile-schema signer attrs)))

    (context "/:username" [username :as {:keys [app-claim identity
                                                reset-claim]}]
      (PUT "/" [new-password]
        (if reset-claim
          (authorize-reset reset-claim username
            (fn [{:keys [username] :as claim}]
              (let [profile (db/fetch db claim)]
                (update-password db profile-schema signer profile
                                 new-password))))
          (authorize-profile identity username
            (fn [profile]
              (update-password db profile-schema signer profile
                               new-password)))))

      (DELETE "/" []
        (authorize-profile identity username
          (fn [profile]
            (when (db/delete! db username)
              (deleted username)))))

      (POST "/auth" []
        (authorize-profile identity username
          (fn [profile]
            (show-auth-token signer profile))))

      (POST "/reset" []
        (authorize-app app-claim username
          (fn [{:keys [username] :as claim}]
            (let [profile (db/fetch db username)]
              (show-reset-token signer profile))))))))
