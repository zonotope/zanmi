(ns zanmi.system
  (:require [zanmi.component.database :refer [database]]
            [zanmi.component.keypair :refer [keypair]]
            [zanmi.component.logger :refer [timbre]]
            [zanmi.component.signer :refer [signer]]
            [zanmi.data.profile :refer [profile-schema]]
            [zanmi.endpoint.profile-endpoint :refer [profile-routes]]
            [zanmi.util.middleware :refer [wrap-format wrap-logger]]
            [com.stuartsierra.component :as component]
            [duct.component.endpoint :refer [endpoint-component]]
            [duct.component.handler :refer [handler-component]]
            [duct.middleware.not-found :refer [wrap-not-found]]
            [meta-merge.core :refer [meta-merge]]
            [ring.component.jetty :refer [jetty-server]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]))

(def base-config
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
                                            :not-modified-responses true}})}})

(defn new-system [config]
  (let [config (meta-merge base-config config)]
    (-> (component/system-map
         :app              (handler-component (:app config))
         :db               (database (:db config))
         :http             (jetty-server (:http config))
         :keypair          (keypair (:keypair config))
         :logger           (timbre (:logger config))
         :profile-endpoint (endpoint-component profile-routes)
         :profile-schema   (profile-schema (:profile-schema config))
         :signer           (signer (:signer config)))

        (component/system-using
         {:app              [:logger :profile-endpoint]
          :http             [:app]
          :profile-endpoint [:db :keypair :logger :profile-schema :signer]}))))
