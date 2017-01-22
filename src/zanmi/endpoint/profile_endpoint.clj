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

(defn- create-profile [attrs & {:keys [db schema signer]}]
  (-> (create schema attrs)
      (as-> validated (db/save! db validated))
      (match {:ok new-profile} (created new-profile signer)
             {:error messages} (error messages 409))))

(defn- update-password [profile new-password & {:keys [db schema signer]}]
  (let [username (:username profile)]
    (-> (update schema profile new-password)
        (as-> validated (db/set! db username validated))
        (match {:ok new-profile} (ok new-profile signer)
               {:error messages} (error messages 400)))))

(defn- delete-profile [{:keys [username] :as profile} & {db :db}]
  (when (db/delete! db username)
    (deleted username)))

(defn- show-auth-token [profile & {:keys [signer]}]
  (ok profile signer))

(defn- show-reset-token [profile & {:keys [signer]}]
  (reset profile signer))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; request authorization                                                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- authorize [credentials username & {:keys [action unauth-message]}]
  (if credentials
    (if (= (:username credentials) username)
      (action credentials)
      (error "unauthorized" 409))
    (error unauth-message 401)))

(defn- authorize-profile [profile username action]
  (authorize profile username
             :action action :unauth-message "bad username or password"))

(defn- authorize-reset [reset-claims username action]
  (authorize reset-claims username
             :action action :unauth-message "invalid reset token"))

(defn- authorize-app [app-claims username action]
  (authorize app-claims username
             :action action :unauth-message "invalid app token"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; routes                                                                   ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn profile-routes [{:keys [db profile-schema signer] :as endpoint}]
  (context route-prefix []
    (POST "/" [profile]
      (create-profile profile :db db :schema profile-schema :signer signer))

    (context "/:username" [username :as {:keys [app-claims reset-claims
                                                user-profile]}]
      (PUT "/" [profile]
        (let [{:keys [password]} profile]
          (if reset-claims
            (authorize-reset reset-claims username
              (fn [{:keys [username] :as claims}]
                (let [profile (db/fetch db username)]
                  (update-password profile password
                                   :db db :schema profile-schema
                                   :signer signer))))
            (authorize-profile user-profile username
              (fn [profile]
                (update-password profile password
                                 :db db :schema profile-schema
                                 :signer signer))))))

      (DELETE "/" []
        (authorize-profile user-profile username
          (fn [profile]
            (delete-profile profile :db db))))

      (POST "/auth" []
        (authorize-profile user-profile username
          (fn [profile]
            (show-auth-token profile :signer signer))))

      (POST "/reset" []
        (authorize-app app-claims username
          (fn [{:keys [username] :as claims}]
            (let [profile (db/fetch db username)]
              (show-reset-token profile :signer signer))))))))
