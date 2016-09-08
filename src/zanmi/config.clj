(ns zanmi.config
  (:require [environ.core :refer [env]]
            [meta-merge.core :refer [meta-merge]]))

(def defaults
  ^:displace {:http {:port 3000}})

(def environ
  {:db {:engine (env :db-engine)
        :username (env :db-username)
        :password (env :db-password)
        :host (env :db-host)
        :db-name (env :db-name)}

   :http {:port (some-> env :port Integer.)}

   :logger {:level (symbol (env :log-level))
            :path (env :log-path)
            :pattern (symbol (env :log-pattern))}

   :profile-repo {:username-length (env :username-length)
                  :password-length (env :password-length)
                  :password-score (env :password-score)}

   :secret (env :secret)})
