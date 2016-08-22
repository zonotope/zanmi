(ns zanmi.endpoint.profile
  (:require [zanmi.data.profile :refer [authenticate create! delete! update!]]
            [zanmi.view.profile :refer [auth-error deleted-message render-error
                                        render-token]]
            [compojure.core :refer [context DELETE GET PUT POST]]
            [ring.util.response :refer [created response]]))

(def ^:private route-prefix "/profiles")

(defn resource-url [{username :username :as profile}]
  (str route-prefix "/" username))

(defn- ok [profile secret]
  (response (render-token profile secret)))

(defn- when-authenticated [db username password response-fn]
  (if-let [profile (authenticate db username password)]
    (response-fn profile)
    (-> (response auth-error)
        (assoc :status 401))))

(defn profile-endpoint [secret]
  (fn [{db :db :as endpoint}]
    (context route-prefix []
      (POST "/" [username password]
        (let [result (create! db {:username username, :password password})]
          (if-let [profile (:ok result)]
            (created (resource-url profile)
                     (render-token profile secret))
            (-> (response (render-error (:error result)))
                (assoc :status 409)))))

      (GET "/:username" [username password]
        (when-authenticated db username password
                            (fn [profile] (ok profile secret))))

      (PUT "/:username" [username password new-password]
        (when-authenticated db username password
                            (fn [_]
                              (let [result (update! db username new-password)]
                                (if-let [new-profile (:ok result)]
                                  (ok new-profile secret)
                                  (-> (:error result)
                                      (render-error)
                                      (response)
                                      (assoc :status 400)))))))

      (DELETE "/:username" [username password]
        (when-authenticated db username password
                            (fn [_] (when (delete! db username)
                                     (-> (deleted-message username)
                                         (response)))))))))
