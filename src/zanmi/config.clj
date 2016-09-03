(ns zanmi.config
  (:require [zanmi.util.middleware :refer [wrap-format wrap-logger]]
            [duct.middleware.not-found :refer [wrap-not-found]]
            [environ.core :refer [env]]
            [meta-merge.core :refer [meta-merge]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]))

(def defaults
  {:app {:middleware [[wrap-format :formats]
                      [wrap-not-found :not-found]
                      [wrap-defaults :defaults]
                      [wrap-logger :logger]]

         :not-found "Resource Not Found"

         :formats [:json :transit-json]

         :defaults (meta-merge api-defaults
                               {:params {:keywordize true
                                         :nested true}
                                :responses {:absolute-redirects true
                                            :not-modified-responses true}})}

   :db {:username "zanmi"}

   :http {:port 3000}

   :logger {:level :info
            :path "log/zanmi.log"
            :pattern :daily}

   :profile-repo {:username-length 32
                  :password-length 64
                  :password-score 3}

   :secret "nobody knows this!"})


(def environ
  {:http {:port (some-> env :port Integer.)}

   :db {:server-name (env :database-host)
        :database-name (env :database-name)
        :password (env :database-password)}

   :secret (env :secret)})
