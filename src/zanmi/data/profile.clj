(ns zanmi.data.profile
  (:require [zanmi.boundary.database :as database]
            [zanmi.util.validation :refer [defvalidator explain-validators]]
            [clojure.spec :as spec]
            [clojure.string :as string]
            [buddy.hashers :as hash]
            [clj-uuid :as uuid]
            [zxcvbn.core :as zxcvbn]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; validation                                                               ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defvalidator string? [field]
  (clojure.core/string? field)
  "Must be a string.")

(defvalidator short-username? [username]
  (<= (count username) 32)
  "The username can't be longer than 32 characters.")

(defvalidator strong-password? [password]
  (>= (:score (zxcvbn/check password)) 3)
  "The password isn't strong enough.")

(spec/def ::username (spec/and string? short-username?))
(spec/def ::password (spec/and string? strong-password?))
(spec/def ::profile (spec/keys :req-un [::username ::password]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; utilities                                                                ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- when-valid [spec data validated-fn]
  (if-let [errors (explain-validators spec data)]
    {:error errors}
    {:ok (validated-fn data)}))

(defn- hash-password [{:keys [password] :as attrs}]
  (-> attrs
      (dissoc :password)
      (assoc :hashed-password (hash/derive password))))

(defn- with-id [{:keys [username] :as attrs}]
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
                              (with-id)
                              (hash-password)
                              (database/create! db)))))

(defn update! [db username new-password]
  (when-valid ::password new-password
              (fn [password] (->> {:password password}
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
