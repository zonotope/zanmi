(ns zanmi.endpoint.profile
  (:require [zanmi.boundary.database :as db]
            [zanmi.data.profile :refer [authenticate create! delete! fetch
                                        update!]]
            [zanmi.view.profile :refer [render-error render-message
                                        render-token]]
            [clojure.core.match :refer [match]]
            [compojure.core :refer [context DELETE GET POST PUT]]
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

(defn- created [profile secret]
  (response/created (resource-url profile)
                    (render-token profile secret)))

(defn- deleted [username]
  (let [deleted-message (format "profile for '%s' deleted" username)]
    (response (render-message deleted-message))))

(defn- error [e status]
  (-> (response (render-error e))
      (assoc :status status)))

(defn- ok [profile secret]
  (response (render-token profile secret)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; auth                                                                     ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- when-authenticated [db username password response-fn]
  (if-let [profile (-> (db/fetch db username)
                       (authenticate password))]
    (response-fn profile)
    (error "bad username or password" 401)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; endpoint                                                                 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn profile-endpoint [secret]
  (fn [{:keys [db profile-schema] :as endpoint}]
    (context route-prefix []
      (POST "/" [username password]
        (-> (create profile-schema {:username username, :password password})
            (match {:ok profile} (created profile secret)
                   {:error messages} (error messages 409))))

      (GET "/:username" [username password]
        (when-authenticated db username password
                            (fn [profile] (ok profile secret))))

      (PUT "/:username" [username password new-password]
        (when-authenticated db username password
                            (fn [profile]
                              (-> (update! profile-schema profile new-password)
                                  (match {:ok new-profile} (ok new-profile secret)
                                         {:error messages} (error messages 400))))))

      (DELETE "/:username" [username password]
        (when-authenticated db username password
                            (fn [profile] (when (db/delete! db profile)
                                           (deleted username))))))))
