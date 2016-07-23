(ns zanmi.config
  (:require [environ.core :refer [env]]))

(def defaults
  {:http {:port 3000}
   :db {:username "zanmi"}})

(def environ
  {:http {:port (some-> env :port Integer.)}
   :db   {:server-name (env :database-host)
          :database-name (env :database-name)
          :password (env :database-password)}})
