(ns zanmi.endpoint.profile
  (:require [zanmi.profile :refer [create! delete! update! valid?]
             compojure.core :refer [DELETE PUT POST]]))

(defn profile-endpoint [{:keys [db] :as endpoint}]
  (context "/profiles" []
    (POST "/" [user pass]
      (create! user pass))
    (PUT "/profiles/:user" [user pass new-pass]
      (when (valid? pass user db)
        (update! user new-pass)))
    (DELETE "profiles/:user" [user pass]
      (when (valid? pass user db)
        (delete! db user)))))
