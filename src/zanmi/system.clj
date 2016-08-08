(ns zanmi.system
  (:require [com.stuartsierra.component :as component]
            [zanmi.component.database :refer [database]]
            [duct.component.endpoint :refer [endpoint-component]]
            [duct.component.handler :refer [handler-component]]
            [duct.middleware.not-found :refer [wrap-not-found]]
            [meta-merge.core :refer [meta-merge]]
            [ring.component.jetty :refer [jetty-server]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [zanmi.endpoint.profile :refer [profile-endpoint]]))

(defn- wrap-format [handler formats]
  (wrap-restful-format handler :formats formats))

(def base-config
  {:app {:middleware [[wrap-format :formats]
                      [wrap-not-found :not-found]
                      [wrap-defaults :defaults]]

         :not-found  "Resource Not Found"

         :formats [:json :transit-json]

         :defaults   (meta-merge api-defaults
                                 {:params {:keywordize true
                                           :nested true}
                                  :responses {:absolute-redirects true
                                              :not-modified-responses true}})

         :secret "nobody knows this!"}})

(defn new-system [config]
  (let [config (meta-merge base-config config)]
    (-> (component/system-map
         :app  (handler-component (:app config))
         :http (jetty-server (:http config))
         :db   (database (:db config))
         :profile (endpoint-component (profile-endpoint (:secret config))))

        (component/system-using
         {:http [:app]
          :app  [:profile]
          :profile [:db]}))))
