(ns zanmi.data.profile
  (:require [zanmi.boundary.database :as database]
            [zxcvbn.core :as zxcvbn]
            [zanmi.util :refer [zxcvbn]]
            [clojure.spec :as spec]
            [buddy.hashers :as hash]
            [clj-uuid :as uuid]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; specs                                                                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- short-username? [username]
  (<= (count username) 32))

(defn- strong-password? [password]
  (>= (:score (zxcvbn/check password))
      3))

(spec/def ::username (spec/and string? short-username?))
(spec/def ::password (spec/and string? strong-password?))
(spec/def ::profile (spec/keys :req-un [::username ::password]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; utilities                                                                ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- when-valid [spec data valid-fn]
  (if (spec/valid? spec data)
    {:ok (valid-fn data)}
    {:errors (spec/explain-data spec data)}))

(defn- hash-password [{:keys [password] :as attrs}]
  (-> attrs
      (dissoc :password)
      (assoc :hashed-password (hash/derive password))))

(defn- add-id [{:keys [username] :as attrs}]
  (let [id (uuid/v5 uuid/+namespace-url+ username)]
    (assoc attrs :id id)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; crud                                                                     ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn fetch [db username]
  (database/fetch db username))

(defn create! [db {:keys [username password] :as attrs}]
  (when-valid ::profile attrs
              (fn [attrs] (->> attrs
                              (add-id)
                              (hash-password)
                              (database/create! db)))))

(defn update! [db username new-password]
  (when-valid ::password new-password
              (fn [password] (->> {:password new-password}
                                 (hash-password)
                                 (database/update! db username)))))

(defn delete! [db username]
  (database/delete! db username))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; auth                                                                     ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn authenticate [db username password]
  (let [{:keys [hashed-password] :as profile} (fetch db username)]
    (when (hash/check password hashed-password)
      profile)))
