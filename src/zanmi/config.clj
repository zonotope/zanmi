(ns zanmi.config
  (:require [environ.core :refer [env]]
            [meta-merge.core :refer [meta-merge]]))

(def defaults
  ^:displace {:http {:port 3000}})

(def environ
  {:db {:engine (some-> env :db-engine symbol)
        :username (:db-username env)
        :password (:db-password env)
        :host (:db-host env)
        :db-name (:db-name env)}

   :http {:port (some-> env :port Integer.)}

   :logger {:level (some-> env :log-level symbol)
            :path (:log-path env)
            :pattern (some-> env :log-pattern symbol)}

   :profile-schema {:username-length (some-> env :username-length Integer.)
                  :password-length (some-> env :password-length Integer.)
                  :password-score (some-> env :password-score Integer.)}

   :secret (:secret env)})
