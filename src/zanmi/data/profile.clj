(ns zanmi.data.profile
  (:require [zanmi.component.schema :refer [schema-component]]
            [zanmi.util.validation :refer [when-valid]]
            [bouncer.validators :refer [defvalidator max-count required string]]
            [buddy.hashers :as hash]
            [clj-uuid :as uuid]
            [clojure.string :as string]
            [zxcvbn.core :as zxcvbn]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; attribute sanitzation                                                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- with-id [{:keys [username] :as attrs}]
  (let [id (uuid/v5 uuid/+namespace-url+ username)]
    (assoc attrs :id id)))

(defn- hash-password [{:keys [password] :as attrs}]
  (-> attrs
      (dissoc :password)
      (assoc :hashed-password (hash/derive password))))

(defn- reset-password [profile new-password]
  (-> profile
      (dissoc :hashed-password)
      (assoc :password new-password)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; crud/auth                                                                ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create [schema attrs]
  (let [create-attrs (with-id attrs)]
    (when-valid create-attrs schema
                (fn [valid-attrs] (hash-password valid-attrs)))))

(defn create! [{:keys [db schema] :as schema} attrs]
  (let [validated (create schema attrs)]
    (database/save! db validated)))

(defn delete! [{db :db} {:keys [username]}]
  (database/delete! db username))

(defn fetch [{db :db} username]
  (database/fetch db username))

(defn update [schema profile new-password]
  (let [update-attrs (reset-password profile new-password)]
    (when-valid update-attrs schema
                (fn [valid-attrs] (-> (hash-password valid-attrs)
                                     (select-keys [:hashed-password]))))))

(defn update! [{:keys [db schema]} {:keys [username] :as profile} new-password]
  (let [validated (update schema profile new-password)]
    (database/set! db username validated)))

(defn authenticate [{:keys [hashed-password] :as profile} password]
  (when (hash/check password hashed-password)
    profile))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; password validation                                                      ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- password-error-message [path value]
  (let [{:keys [suggestions warning]} (:feedback (zxcvbn/check value))
        path-name (name (peek path))]
    (str "The " path-name " is too weak. "
         warning " " (string/join " " suggestions))))

(defvalidator min-password-score {:message-fn password-error-message}
  [password strength]
  (>= (:score (zxcvbn/check password))
      strength))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; data schema                                                                ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn profile-schema [{:keys [username-length password-length password-score]}]
  (let [schema {:username [required
                           string
                           [max-count username-length]]

                :password [required
                           string
                           [max-count password-length]
                           [min-password-score password-score]]}]

    (schema-component schema)))
