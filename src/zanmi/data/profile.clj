(ns zanmi.data.profile
  (:require [zanmi.boundary.database :as database]
            [buddy.hashers :as hash]
            [clj-uuid :as uuid]))

(defn- hash-password [{:keys [password] :as attrs}]
  (-> attrs
      (dissoc :password)
      (assoc :hashed-password (hash/derive password))))

(defn- add-id [{:keys [username] :as attrs}]
  (let [id (uuid/v5 uuid/+namespace-url+ username)]
    (assoc attrs :id id)))

(defn fetch [db username]
  (database/fetch db username))

(defn create! [db {:keys [username password] :as attrs}]
  (->> attrs
       (add-id)
       (hash-password)
       (database/create! db)))

(defn update! [db username new-password]
  (->> {:password new-password}
       (hash-password)
       (database/update! db username)))

(defn delete! [db username]
  (database/delete! db username))

(defn valid? [db username password]
  (let [{:keys [hashed-password] :as profile} (fetch db username)]
    (when (hash/check password hashed-password)
      profile)))
