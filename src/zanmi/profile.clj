(ns zanmi.profile
  (:require [zanmi.boundary.database :as database]
            [buddy.hashers :as hash]
            [clj-uuid :as uuid]))

(defn- hash-password [{:keys [password] :as attrs}]
  (-> attrs
      (dissoc :password)
      (assoc :hashed-password (hash/derive password))))

(defn- add-id [{username :username :as attrs}]
  (let [id (uuid/v5 uuid/+namespace-url+ username)]
    (assoc attrs :id id)))

(defn get [db username]
  (database/get db username))

(defn create! [db {:keys [username password] :as attrs}]
  (->> attrs
       (add-id)
       (hash-password)
       (database/create! db)))

(defn update! [db username new-password]
  (let [saved-profile (get db username)]
    (database/update! db username (hash-password {:password new-password}))))

(defn delete! [db username]
  (database/delete! db username))

(defn valid? [password {:keys [hashed-password] :as profile}]
  (hash/check password hashed-password))
