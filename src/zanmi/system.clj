(ns zanmi.system
  (:require [zanmi.component.database :refer [database]]
            [zanmi.component.logger :refer [timbre]]
            [zanmi.data.profile :refer [profile-schema]]
            [zanmi.endpoint.profile :refer [profile-endpoint]]
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
         :logger           (timbre (:logger config))
         :profile-endpoint (endpoint-component
                            (profile-endpoint (:secret config)))
         :profile-schema   (profile-schema (:profile-schema config)))

        (component/system-using
         {:app              [:logger :profile-endpoint]
          :http             [:app]
          :profile-endpoint [:logger :profile-schema]
          :profile-schema   [:db]}))))
