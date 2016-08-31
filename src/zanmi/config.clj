(ns zanmi.config
  (:require [environ.core :refer [env]]))

(def defaults
  {:http {:port 3000}
   :db {:username "zanmi"}
   :password-strength 3
   :username-length 32})

(def environ
  {:http {:port (some-> env :port Integer.)}
   :db   {:server-name (env :database-host)
          :database-name (env :database-name)
          :password (env :database-password)}
   :username-length (env :username-length)
   :password-score (env :password-score))}
