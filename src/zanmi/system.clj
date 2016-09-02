(ns zanmi.system
  (:require [zanmi.component.database :refer [database]]
            [zanmi.component.logger :refer [logger]]
            [zanmi.data.profile :refer [profile-repo]]
            [zanmi.endpoint.profile :refer [profile-endpoint]]
            [com.stuartsierra.component :as component]
            [duct.component.endpoint :refer [endpoint-component]]
            [duct.component.handler :refer [handler-component]]
            [duct.middleware.not-found :refer [wrap-not-found]]
            [meta-merge.core :refer [meta-merge]]
            [ring.component.jetty :refer [jetty-server]]
            [ring.logger :refer [wrap-with-logger]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.format :refer [wrap-restful-format]]))

(defn- wrap-format [handler formats]
  (wrap-restful-format handler :formats formats))

(defn- wrap-logger [handler logger]
  (wrap-with-logger handler {:logger logger}))

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
                                            :not-modified-responses true}})}

   :logger {}

   :profile-repo {:username-length 32
                  :password-length 64
                  :password-score 3}

   :secret "nobody knows this!"})

(defn new-system [config]
  (let [config (meta-merge base-config config)]
    (-> (component/system-map
         :app              (handler-component (:app config))
         :db               (database (:db config))
         :http             (jetty-server (:http config))
         :logger           (loger (:logger config))
         :profile-endpoint (endpoint-component
                            (profile-endpoint (:secret config)))
         :profile-repo     (profile-repo (:profile-repo config)))

        (component/system-using
         {:app              [:profile-endpoint :logger]
          :http             [:app]
          :profile-endpoint [:logger :profile-repo]
          :profile-repo     [:db]}))))
