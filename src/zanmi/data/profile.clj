(ns zanmi.data.profile
  (:require [zanmi.boundary.database :as database]
            [zanmi.component.repo :refer repo]
            [bouncer.core :as bouncer]
            [bouncer.validators :as validators]
            [buddy.hashers :as hash]
            [clj-uuid :as uuid]
            [clojure.string :as string]
            [zxcvbn.core :as zxcvbn]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; validation                                                               ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- short-username? [username length]
  (<= (count username)
      length))

(defn- strong-password? [password strength]
  (>= (:score (zxcvbn/check password))
      strength))

(defn- when-valid [data schema validated-fn]
  (let [[errors validated] (bouncer/validate data schema)]
    (if errors
      {:error errors}
      {:ok (validated-fn validated)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; utility fns                                                              ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn fetch [{db :db} username]
  (database/fetch db username))

(defn create! [{:keys [db schema]} {password :password :as attrs}]
  (when-valid attrs schema
              (fn [valid-attrs] (->> valid-attrs
                                    (with-id)
                                    (hash-password)
                                    (database/create! db)))))

(defn update! [{:keys [db schema]} username new-password]
  (let [attr {:password new-password}
        validator (select-keys [:password] schema)]
    (when-valid attr validator
                (fn [valid-attr] (->> valid-attr
                                     (hash-password)
                                     (database/update! db username))))))

(defn delete! [{db :db} username]
  (database/delete! db username))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; auth                                                                     ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn authenticate [{:keys [db schema]} username password]
  (let [{:keys [hashed-password] :as profile} (fetch db username)]
    (when (hash/check password hashed-password)
      profile)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; repo                                                                     ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn profile-repo [{:keys [username-length password-score]} :as config]
  (let [schema {:username [validators/required
                           validators/string
                           [short-username? username-length]]
                :password [validators/required
                           validators/string
                           [strong-password? password-score]]}]
    (repo schema)))
