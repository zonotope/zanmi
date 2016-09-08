(ns zanmi.config
  (:require [environ.core :refer [env]]
            [meta-merge.core :refer [meta-merge]]))

(def defaults
  ^:displace {:http {:port 3000}})

(def environ
  {:db {:server-name (env :database-host)
        :database-name (env :database-name)
        :password (env :database-password)}

   :http {:port (some-> env :port Integer.)}

   :logger {:level (symbol (env :log-level))
            :path (env :log-path)
            :pattern (symbol (env :log-pattern))}

   :profile-repo {:username-length (env :username-length)
                  :password-length (env :password-length)
                  :password-score (env :password-score)}

   :secret (env :secret)})
