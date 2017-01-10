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

(defn- authorize [credentials username & {:keys [action unauth-message]} ]
  (if credentials
    (if (= (:username credentials) username)
      (action credentials)
      (error "unauthorized" 409))
    (error unauth-message 401)))

(defn- authorize-profile [profile username action]
  (authorize profile username
             :action action :unauth-message "bad username or password"))

(defn- authorize-reset [reset-claim username action]
  (authorize reset-claim username
             :action action :unauth-message "invalid reset token"))

(defn- authorize-app [app-claim username action]
  (authorize app-claim username
             :action action :unauth-message "invalid app token"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; routes                                                                   ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn profile-routes [{:keys [db profile-schema signer] :as endpoint}]
  (context route-prefix []
    (POST "/" [profile]
      (create-profile db profile-schema signer profile))

    (context "/:username" [username :as {:keys [app-claim identity
                                                reset-claim]}]
      (PUT "/" [profile]
        (let [{:keys [password]} profile]
          (if reset-claim
            (authorize-reset reset-claim username
              (fn [{:keys [username] :as claim}]
                (let [profile (db/fetch db claim)]
                  (update-password db profile-schema signer profile password))))
            (authorize-profile identity username
              (fn [profile]
                (update-password db profile-schema signer profile password))))))

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
