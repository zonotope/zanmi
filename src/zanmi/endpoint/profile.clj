(ns zanmi.endpoint.profile
  (:require [zanmi.data.profile :refer [get create! delete! update! valid?]]
            [zanmi.view.profile :refer [render-message render-token]]
            [compojure.core :refer [context DELETE GET PUT POST]]
            [ring.util.response :refer [created response]]))

(defn- resource-url [username]
  (str "/profiles/" username))

(defn- ok [profile secret]
  (response (render-token profile secret)))

(defn- when-authenticated [db username password response-fn]
  (if-let [profile (valid? db username password)]
    (response-fn profile)
    (-> (response (render-message "bad username or password"))
        (assoc :status 401))))

(defn profile-endpoint [secret]
  (fn [{db :db :as endpoint}]
    (context "/profiles" []
      (POST "/" [username password]
        (if-let [profile (create! db {:username username, :password password})]
          (created (resource-url username)
                   (render-token profile secret))
          (-> (response (render-message "username is already taken"))
              (assoc :status 409))))

      (GET "/:username" [username password]
        (when-authenticated db username password
                         (fn [profile] (ok profile secret))))

      (PUT "/:username" [username password new-password]
        (when-authenticated db username password
                         (fn [_]
                           (let [new-profile (update! db username new-password)]
                             (ok new-profile secret)))))

      (DELETE "/:username" [username password]
        (when-authenticated db username password
                         (fn [_] (when (delete! db username)
                                  (-> (render-message (str username " deleted"))
                                      (response)))))))))
