(ns zanmi.endpoint.profile
  (:require [zanmi.data.profile :refer [authenticate create! delete! update!]]
            [zanmi.view.profile :refer [render-error render-message
                                        render-token]]
            [compojure.core :refer [context DELETE GET PUT POST]]
            [ring.util.response :as response :refer [response]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; url                                                                      ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private route-prefix "/profiles")

(defn- resource-url [{username :username :as profile}]
  (str route-prefix "/" username))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; responses                                                                ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- ok [profile secret]
  (response (render-token profile secret)))

(defn- created [profile secret]
  (response/created (resource-url profile)
                    (render-token profile secret)))

(defn- deleted [username]
  (let [deleted-message (str username " deleted")]
    (response (render-message (deleted-message username)))))

(defn- error [e status]
  (-> (response (render-error e))
      (assoc :status status)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; auth                                                                     ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- when-authenticated [repo username password response-fn]
  (if-let [profile (authenticate repo username password)]
    (response-fn profile)
    (error "bad username or password" 401)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; endpoint                                                                 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn profile-endpoint [secret]
  (fn [{repo :repo :as endpoint}]
    (context route-prefix []
      (POST "/" [username password]
        (let [result (create! repo {:username username, :password password})]
          (if-let [profile (:ok result)]
            (created profile secret)
            (error (:error result) 409))))

      (GET "/:username" [username password]
        (when-authenticated repo username password
                            (fn [profile] (ok profile secret))))

      (PUT "/:username" [username password new-password]
        (when-authenticated repo username password
                            (fn [_]
                              (let [result (update! repo username new-password)]
                                (if-let [new-profile (:ok result)]
                                  (ok new-profile secret)
                                  (error (:error result) 400))))))

      (DELETE "/:username" [username password]
        (when-authenticated repo username password
                            (fn [_] (when (delete! repo username)
                                     (deleted username))))))))
