(ns zanmi.repo.profile
  (:require [zanmi.boundary.database :as database]
            [zanmi.component.repo :refer [repo-component]]
            [bouncer.core :as bouncer]
            [bouncer.validators :as validators]
            [buddy.hashers :as hash]
            [clj-uuid :as uuid]
            [clojure.string :as string]
            [zxcvbn.core :as zxcvbn]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; validation                                                               ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- strong-password? [password strength]
  (>= (:score (zxcvbn/check password))
      strength))

(defn- password-message [{:keys [path value]}]
  (let [{{:keys [suggestions warning]} :feedback} (zxcvbn/check value)]
    (str "The " path " isn't strong enough. "
         warning " "
         (string/join " " suggestions))))

(defn- profile-message-fn [{:keys [path message value]
                            {:keys [default-message-format]} :metadata
                            :as validation-result}]
  (cond (= [:password] path) (password-message [validation-result])
        message message
        :else (format default-message-format value)))

(defn- when-valid [data schema validated-fn]
  (let [[errors validated] (bouncer/validate profile-message-fn data schema)]
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
        validator (select-keys schema [:password])]
    (when-valid attr validator
                (fn [valid-attr] (->> valid-attr
                                     (hash-password)
                                     (database/update! db username))))))

(defn delete! [{db :db} username]
  (database/delete! db username))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; auth                                                                     ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn authenticate [{db :db} username password]
  (let [{:keys [hashed-password] :as profile} (fetch db username)]
    (when (hash/check password hashed-password)
      profile)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; repo                                                                     ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn profile-repo [{:keys [username-length password-score]}]
  (let [schema {:username [validators/required
                           validators/string
                           [validators/max-count username-length]]

                :password [validators/required
                           validators/string
                           [strong-password? password-score]]}]

    (repo-component schema)))
