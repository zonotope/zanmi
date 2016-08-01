(ns zanmi.endpoint.profile
  (:require [zanmi.profile :refer [get create! delete! update! valid?]]
            [buddy.sign.jwt :as jwt]
            [compojure.core :refer [context DELETE GET PUT POST]]))

(defn- sign-profile [profile secret]
  (-> profile
      (select-keys [:id :username])
      (jwt/sign secret)))

(defn profile-endpoint [secret]
  (fn [{db :db :as endpoint}]
    (context "/profiles" []
      (POST "/" [username password]
        (-> (create! db {:username username, :password password})
            (sign-profile secret)))

      (GET "/:username" [username password]
        (when-let [profile (valid? db username password)]
          (sign-profile profile secret)))

      (PUT "/:username" [username password new-password]
        (when (valid? db username password)
          (-> (update! db username new-password)
              (sign-profile secret))))

      (DELETE "/:username" [username password]
        (when (valid? db username password)
          (delete! db username))))))
