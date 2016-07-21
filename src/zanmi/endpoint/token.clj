(ns zanmi.endpoint.token
  (:require [zanmi.profile :refer [get valid?]]
            [buddy.sign.jwt :as jwt]
            [compojure.core :refer [POST]]))

(defn- build-token [profile secret]
  (-> profile
      (select-keys [:id :username])
      (jwt/sign secret))))

(defn token-endpoint [{:keys [db secret] :as endpoint}]
  (context "/tokens" []
    (POST "/" [user pass]
      (let [profile (get db user)]
        (when (valid? pass profile)
          (build-token profile secret))))))
