(ns zanmi.config
  (:require [environ.core :refer [env]]
            [meta-merge.core :refer [meta-merge]]))

(def defaults
  ^:displace {:http {:port 3000}})

(def environ
  {:db {:engine (:db-engine env)
        :username (:db-username env)
        :password (:db-password env)
        :host (:db-host env)
        :db-name (:db-name env)}

   :http {:port (some-> env :port Integer.)}

   :logger {:level (some-> env :log-level symbol)
            :path (:log-path env)
            :pattern (some-> env :log-pattern symbol)}

   :profile-repo {:username-length (:username-length env)
                  :password-length (:password-length env)
                  :password-score (:password-score env)}

   :secret (:secret env)})
