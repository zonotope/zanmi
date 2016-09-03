(ns zanmi.system
  (:require [zanmi.component.database :refer [database]]
            [zanmi.component.logger :refer [timbre-logger]]
            [zanmi.config :as config]
            [zanmi.data.profile :refer [profile-repo]]
            [zanmi.endpoint.profile :refer [profile-endpoint]]
            [com.stuartsierra.component :as component]
            [duct.component.endpoint :refer [endpoint-component]]
            [duct.component.handler :refer [handler-component]]
            [meta-merge.core :refer [meta-merge]]
            [ring.component.jetty :refer [jetty-server]]))

(defn new-system [config]
  (let [config (meta-merge config/defaults config)]
    (-> (component/system-map
         :app              (handler-component (:app config))
         :db               (database (:db config))
         :http             (jetty-server (:http config))
         :logger           (timbre-logger (:logger config))
         :profile-endpoint (endpoint-component
                            (profile-endpoint (:secret config)))
         :profile-repo     (profile-repo (:profile-repo config)))

        (component/system-using
         {:app              [:logger :profile-endpoint]
          :http             [:app]
          :profile-endpoint [:logger :profile-repo]
          :profile-repo     [:db]}))))
