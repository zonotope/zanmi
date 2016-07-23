(ns zanmi.endpoint.profile
  (:require [zanmi.profile :refer [get create! delete! update! valid?]]
            [compojure.core :refer [context DELETE PUT POST]]))

(defn profile-endpoint [{:keys [db] :as endpoint}]
  (context "/profiles" []
    (POST "/" [user pass]
      (create! {:username user, :password pass}))
    (PUT "/profiles/:user" [user pass new-pass]
      (let [profile (get user)]
        (when (valid? pass profile)
          (update! user new-pass))))
    (DELETE "profiles/:user" [user pass]
      (let [profile (get user)]
        (when (valid? pass profile)
          (delete! db user))))))
