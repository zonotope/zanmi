(ns zanmi.middleware.credentials
  (:require [zanmi.util.codec :refer [base64-decode]]
            [clojure.string :as string]))

(defn- parse-credentials [headers]
  (some-> headers
          (get "authorization")
          (as-> header (re-find #"^Basic (.*)$" header))
          (second)
          (base64-decode)
          (string/split #":" 2)
          (as-> creds (zipmap [:username :password] creds))))

(defn wrap-credentials [handler]
  (fn [{:keys [headers] :as req}]
    (handler (assoc req :credentials (parse-credentials headers)))))
