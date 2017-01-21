(ns zanmi.system
  (:require [zanmi.component.database :refer [database]]
            [zanmi.component.immutant :refer [immutant-web-server]]
            [zanmi.component.signer :refer [signer]]
            [zanmi.component.timbre :refer [timbre]]
            [zanmi.component.signer.sha :refer [sha-signer]]
            [zanmi.data.profile :refer [profile-schema]]
            [zanmi.endpoint.profile-endpoint :refer [profile-routes]]
            [zanmi.middleware.authentication :refer [wrap-authentication
                                                     wrap-parse-api-token
                                                     wrap-parse-reset-token]]
            [zanmi.middleware.format :refer [wrap-format]]
            [zanmi.middleware.logger :refer [wrap-logger]]
            [com.stuartsierra.component :as component]
            [duct.component.endpoint :refer [endpoint-component]]
            [duct.component.handler :refer [handler-component]]
            [duct.middleware.not-found :refer [wrap-not-found]]
            [meta-merge.core :refer [meta-merge]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]))

(def base-config
  {:app {:middleware [[wrap-authentication :db]
                      [wrap-parse-api-token :app-validater]
                      [wrap-parse-reset-token :signer]
                      [wrap-defaults :defaults]
                      [wrap-not-found :not-found]
                      [wrap-format :formats]
                      [wrap-logger :logger]]

         :not-found "Resource Not Found"

         :formats [:json :transit-json]

         :defaults (meta-merge api-defaults
                               {:params {:keywordize true
                                         :nested true}
                                :responses {:absolute-redirects true
                                            :not-modified-responses true}})}})

(defn zanmi [config]
  (let [config (meta-merge base-config config)]
    (-> (component/system-map
         :app-validater    (sha-signer {:secret (:api-key config)
                                        :size 512})
         :app              (handler-component (:app config))
         :db               (database (:db config))
         :http             (immutant-web-server (:http config))
         :logger           (timbre (:logger config))
         :profile-endpoint (endpoint-component profile-routes)
         :profile-schema   (profile-schema (:profile-schema config))
         :signer           (signer (:signer config)))

        (component/system-using
         {:app              [:app-validater :db :logger :profile-endpoint
                             :signer]
          :http             [:app]
          :profile-endpoint [:app-validater :db :logger :profile-schema
                             :signer]}))))
