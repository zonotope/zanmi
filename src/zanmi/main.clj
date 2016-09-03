(ns zanmi.main
  (:gen-class)
  (:require [zanmi.config :as config]
            [zanmi.system :refer [new-system]]
            [com.stuartsierra.component :as component]
            [duct.middleware.errors :refer [wrap-hide-errors]]
            [duct.util.runtime :refer [add-shutdown-hook]]
            [ring.middleware.ssl :refer [wrap-hsts wrap-ssl-redirect]]
            [meta-merge.core :refer [meta-merge]]))

(def prod-config
  {:app {:middleware [[wrap-hide-errors :internal-error]
                      wrap-hsts
                      wrap-ssl-redirect]

         :internal-error "Internal Server Error"}})

(def config
  (meta-merge config/environ
              prod-config))

(defn -main [& args]
  (let [system (new-system config)]
    (println "Starting HTTP server on port" (-> system :http :port))
    (add-shutdown-hook ::stop-system #(component/stop system))
    (component/start system)))
