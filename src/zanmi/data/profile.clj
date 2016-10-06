(ns zanmi.data.profile
  (:require [zanmi.boundary.database :as database]
            [zanmi.component.repo :refer [repo-component]]
            [zanmi.util.validation :refer [when-valid]]
            [bouncer.validators :refer [defvalidator max-count required string]]
            [buddy.hashers :as hash]
            [clj-uuid :as uuid]
            [clojure.string :as string]
            [zxcvbn.core :as zxcvbn]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; attribute sanitzation                                                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- hash-password [{:keys [password] :as attrs}]
  (-> attrs
      (dissoc :password)
      (assoc :hashed-password (hash/derive password))))

(defn- with-id [{:keys [username] :as attrs}]
  (let [id (uuid/v5 uuid/+namespace-url+ username)]
    (assoc attrs :id id)))

(defn build [attrs]
  (-> attrs
      (with-id)
      (hash-password)))

(defn reset-password [profile new-password]
  (-> profile
      (dissoc :hashed-password)
      (assoc :password new-password)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; auth                                                                     ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn authenticate [{:keys [hashed-password] :as profile} password]
  (when (hash/check password hashed-password)
    profile))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; crud                                                                     ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create [schema attrs]
  (when-valid attrs schema (fn [valid-attrs] (build valid-attrs))))

(defn create! [{:keys [db schema] :as repo} attrs]
  (let [validated (create schema attrs)]
    (database/save! db validated)))

(defn delete! [{db :db} {:keys [username]}]
  (database/delete! db username))

(defn fetch [{db :db} username]
  (database/fetch db username))

(defn update [schema profile new-password]
  (let [new-attrs (reset-password profile new-password)]
    (when-valid new-attrs schema
                (fn [valid-attrs] (-> (hash-password valid-attrs)
                                     (select-keys [:hashed-password]))))))

(defn update! [{:keys [db schema]} {:keys [username] :as profile} new-password]
  (let [validated (update schema profile new-password)]
    (database/set! db username validated)))

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
;; data repo                                                                ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn profile-repo [{:keys [username-length password-length password-score]}]
  (let [schema {:username [required
                           string
                           [max-count username-length]]

                :password [required
                           string
                           [max-count password-length]
                           [min-password-score password-score]]}]

    (repo-component schema)))
