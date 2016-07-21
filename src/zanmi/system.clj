(ns zanmi.system
  (:require [com.stuartsierra.component :as component]
            [zanmi.component.database :refer [database]]
            [duct.component.endpoint :refer [endpoint-component]]
            [duct.component.handler :refer [handler-component]]
            [duct.component.ragtime :refer [ragtime]]
            [duct.middleware.not-found :refer [wrap-not-found]]
            [meta-merge.core :refer [meta-merge]]
            [ring.component.jetty :refer [jetty-server]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [zanmi.endpoint.example :refer [example-endpoint]]))

(def base-config
  {:app {:middleware [[wrap-not-found :not-found]
                      [wrap-defaults :defaults]]

         :not-found  "Resource Not Found"

         :defaults   (meta-merge api-defaults
                                 {:params {:keywordize true
                                           :nested true}
                                  :responses {:content-types true}})}

   :ragtime {:resource-path "zanmi/migrations"}})

(defn new-system [config]
  (let [config (meta-merge base-config config)]
    (-> (component/system-map
         :app  (handler-component (:app config))
         :http (jetty-server (:http config))
         :db   (database (:db config))
         :ragtime (ragtime (:ragtime config))
         :example (endpoint-component example-endpoint))
        (component/system-using
         {:http [:app]
          :app  [:example]
          :ragtime [:db]
          :example [:db]}))))
