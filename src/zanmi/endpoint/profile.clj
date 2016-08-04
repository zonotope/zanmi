(ns zanmi.endpoint.profile
  (:require [zanmi.data.profile :refer [get create! delete! update! valid?]]
            [buddy.sign.jwt :as jwt]
            [compojure.core :refer [context DELETE GET PUT POST]]
            [ring.util.response :refer [created response]]))

(defn- resource-url [username]
  (str "/profiles/" username))

(defn- sign [profile secret]
  (-> profile
      (select-keys [:id :username])
      (jwt/sign secret)))

(defn- token-response-body [profile secret]
  {:token (sign profile secret)})

(defn- ok-token-response [profile secret]
  (response (token-response-body profile secret)))

(defn- when-authorized [db username password response-fn]
  (if-let [profile (valid? db username password)]
    (response-fn profile)
    (-> (response {:message "bad username or password"})
        (assoc :status 401))))

(defn profile-endpoint [secret]
  (fn [{db :db :as endpoint}]
    (context "/profiles" []
      (POST "/" [username password]
        (if-let [profile (create! db {:username username, :password password})]
          (created (resource-url username)
                   (token-response-body profile secret))
          (-> (response {:message "username is already taken"})
              (assoc :status 409))))

      (GET "/:username" [username password]
        (when-authorized db username password
                         (fn [profile] (ok-token-response profile secret))))

      (PUT "/:username" [username password new-password]
        (when-authorized db username password
                         (fn [_]
                           (let [new-profile (update! db username new-password)]
                             (ok-token-response new-profile secret)))))

      (DELETE "/:username" [username password]
        (when-authorized db username password
                         (fn [_] (when (delete! db username)
                                  (response {:message "deleted"}))))))))
