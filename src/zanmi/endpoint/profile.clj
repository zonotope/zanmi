(ns zanmi.endpoint.profile
  (:require [zanmi.profile :refer [get create! delete! update! valid?]]
            [buddy.sign.jwt :as jwt]
            [compojure.core :refer [context DELETE GET PUT POST]]
            [ring.util.response :as response :refer [content-type response]]))

(defn- resource-url [username]
  (str "/profiles/" username))

(defn- sign-profile [profile secret]
  (-> profile
      (select-keys [:id :username])
      (jwt/sign secret)))

(defn- profile->token [profile secret]
  {:token (sign-profile profile secret)})

(defn- ok [token]
  (-> (response token)
      (content-type "text/json")))

(defn- created [url token]
  (-> (response/created url token)
      (content-type "text/json")))

(defn profile-endpoint [secret]
  (fn [{db :db :as endpoint}]
    (context "/profiles" []
      (POST "/" [username password]
        (let [token (-> (create! db {:username username, :password password})
                        (profile->token secret))
              url (resource-url username)]
          (created url token)))

      (GET "/:username" [username password]
        (when-let [profile (valid? db username password)]
          (-> profile
              (profile->token secret)
              (ok))))

      (PUT "/:username" [username password new-password]
        (when (valid? db username password)
          (-> (update! db username new-password)
              (profile->token secret)
              (ok))))

      (DELETE "/:username" [username password]
        (when (valid? db username password)
          (-> (delete! db username)
              (ok)))))))
