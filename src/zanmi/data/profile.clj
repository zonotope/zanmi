(ns zanmi.data.profile
  (:require [zanmi.boundary.database :as database]
            [clojure.spec :as spec]
            [buddy.hashers :as hash]
            [clj-uuid :as uuid]))

(defn- username-length? [username]
  (<= (count username) 32))

(spec/def ::username (spec/and string? username-length?))
(spec/def ::password string?)
(spec/def ::profile (spec/keys :req-un [::username ::password]))

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
  (when (spec/valid? ::profile attrs)
    (->> attrs
         (add-id)
         (hash-password)
         (database/create! db))))

(defn update! [db username new-password]
  (when (spec/valid? ::password new-password)
    (->> {:password new-password}
         (hash-password)
         (database/update! db username))))

(defn delete! [db username]
  (database/delete! db username))

(defn authenticate [db username password]
  (let [{:keys [hashed-password] :as profile} (fetch db username)]
    (when (hash/check password hashed-password)
      profile)))
