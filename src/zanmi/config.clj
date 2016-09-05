(ns zanmi.config
  (:require [environ.core :refer [env]]
            [meta-merge.core :refer [meta-merge]]))

(def defaults
  ^:displace {:http {:port 3000}})

(def environ
  {:http {:port (some-> env :port Integer.)}

   :db {:server-name (env :database-host)
        :database-name (env :database-name)
        :password (env :database-password)}

   :secret (env :secret)})
