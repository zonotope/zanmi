(ns zanmi.endpoint.profile
  (:require [zanmi.data.profile :refer [get create! delete! update! valid?]]
            [buddy.sign.jwt :as jwt]
            [compojure.core :refer [context DELETE GET PUT POST]]
            [ring.util.response :refer [created response]]))

(defn- resource-url [username]
  (str "/profiles/" username))

(defn- sign-profile [profile secret]
  (-> profile
      (select-keys [:id :username])
      (jwt/sign secret)))

(defn- profile->token [profile secret]
  {:token (sign-profile profile secret)})

(defn- when-authorized [db username password response-fn]
  (if-let [profile (valid? db username password)]
    (response-fn profile)
    (-> (response {:message "bad username or password"})
        (assoc :status 401))))

(defn profile-endpoint [secret]
  (fn [{db :db :as endpoint}]
    (context "/profiles" []
      (POST "/" [username password]
        (if-let [token (-> (create! db {:username username, :password password})
                           (profile->token secret))]
          (created (resource-url username)
                   token)
          (-> (response {:message "username is already taken"})
              (assoc :status 409))))

      (GET "/:username" [username password]
        (when-authorized db username password
                         (fn [profile] (-> profile
                                          (profile->token secret)
                                          (response)))))

      (PUT "/:username" [username password new-password]
        (when-authorized db username password
                         (fn [_] (-> (update! db username new-password)
                                    (profile->token secret)
                                    (response)))))

      (DELETE "/:username" [username password]
        (when-authorized db username password
                         (fn [_] (-> (delete! db username)
                                    (response {:message "deleted"}))))))))
