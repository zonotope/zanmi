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

(def ^:private unauthorized
  (-> (response {:message "bad username or password"})
      (assoc :status 401)))

(def ^:private conflict
  (-> (response {:message "username is already taken"})
      (assoc :status 409)))

(defn profile-endpoint [secret]
  (fn [{db :db :as endpoint}]
    (context "/profiles" []
      (POST "/" [username password]
        (if-let [token (-> (create! db {:username username, :password password})
                           (profile->token secret))]
          (created (resource-url username) token)
          conflict))

      (GET "/:username" [username password]
        (if-let [profile (valid? db username password)]
          (-> profile
              (profile->token secret)
              (ok))
          unauthorized))

      (PUT "/:username" [username password new-password]
        (if (valid? db username password)
          (-> (update! db username new-password)
              (profile->token secret)
              (ok))
          unauthorized))

      (DELETE "/:username" [username password]
        (if (valid? db username password)
          (-> (delete! db username)
              (ok))
          unauthorized)))))
