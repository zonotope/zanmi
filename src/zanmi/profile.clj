(ns zanmi.profile
  (:require [zanmi.boundary.database :as database]
            [buddy.hashers :as hash]))

(defn- hash-password [{:keys [password] :as attrs}]
  (-> attrs
      (dissoc :password)
      (assoc :hashed-password (hash/derive password))))

(defn- get [db username]
  (database/get db username))

(defn create! [db username password]
  (let [hashed-attrs (hash-password attrs)]
    (database/create! db hashed-attrs)))

(defn update! [db username new-password]
  (let [saved-profile (get db username)
        updated-profile (merge saved-profile {:p})]
    (database/update! db updated-profile)))

(defn delete! [db username]
  (database/delete! db username))

(defn valid? [password username db]
  (let [stored-password (:hashed-password (get db username))]
    (hash/check password stored-password)))
