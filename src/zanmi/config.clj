(ns zanmi.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [environ.core :refer [env]]
            [meta-merge.core :refer [meta-merge]])
  (:import java.io.PushbackReader))

(def defaults
  ^:displace {:http {:port 8686}})

(def environ
  {:api-key (:api-key env)

   :db {:engine (some-> env :db-engine symbol)
        :username (:db-username env)
        :password (:db-password env)
        :host (:db-host env)
        :db-name (:db-name env)}

   :http {:port (or (some-> env :port Integer.)
                    8686)}

   :logger {:level (some-> env :log-level symbol)
            :path (:log-path env)
            :pattern (some-> env :log-pattern symbol)}

   :profile-schema {:username-length (some-> env :username-length Integer.)
                    :password-length (some-> env :password-length Integer.)
                    :password-score (some-> env :password-score Integer.)}

   :signer {:algorithm (some-> env :sign-algorithm symbol)
            :size (some-> env :sign-hash-size Integer.)
            :secret (:sign-secret env)
            :keypair {:public  (:sign-public-key env)
                      :private (:sign-private-key env)}
            :auth-expire-after (some-> env :auth-expiration Integer.)
            :reset-expire-after (some-> env :reset-expiration Integer.)}})

(def file
  (when-let [path (:zanmi-config env)]
    (with-open [reader (-> path io/reader PushbackReader.)]
      (edn/read reader))))
