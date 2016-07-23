(ns zanmi.endpoint.token
  (:require [zanmi.profile :refer [get valid?]]
            [buddy.sign.jwt :as jwt]
            [compojure.core :refer [context POST]]))

(defn- build-token [profile secret]
  (-> profile
      (select-keys [:id :username])
      (jwt/sign secret)))

(defn token-endpoint [secret]
  (fn [{db :db :as endpoint}]
    (context "/tokens" []
      (POST "/" [user pass]
        (let [profile (get db user)]
          (when (valid? pass profile)
            (build-token profile secret)))))))
