(ns zanmi.data.profile
  (:require [zanmi.util.validation :refer [when-valid]]
            [bouncer.validators :refer [defvalidator max-count required string]]
            [buddy.hashers :as hash]
            [clojure.string :as string]
            [zxcvbn.core :as zxcvbn]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; attribute sanitzation                                                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- with-id [{:keys [username] :as attrs}]
  (let [id (java.util.UUID/randomUUID)]
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
;; profile processing                                                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn authenticate [{:keys [hashed-password] :as profile} password]
  (when (hash/check password hashed-password)
    profile))

(defn create [schema attrs]
  (let [create-attrs (with-id attrs)]
    (when-valid create-attrs schema
                (fn [valid-attrs] (hash-password valid-attrs)))))

(defn update [schema profile new-password]
  (let [update-attrs (reset-password profile new-password)]
    (when-valid update-attrs schema
                (fn [valid-attrs] (-> (hash-password valid-attrs)
                                     (select-keys [:hashed-password]))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; profile validation                                                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defvalidator no-colon {:default-message-format "%s can't have a ':'"}
  [username]
  (not (string/includes? username ":")))

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
;; data schema                                                              ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn profile-schema [{:keys [username-length password-length password-score]}]
  {:username [required
              string
              no-colon
              [max-count username-length]]

   :password [required
              string
              [max-count password-length]
              [min-password-score password-score]]})
