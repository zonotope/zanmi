(ns zanmi.main
  (:gen-class)
  (:require [zanmi.boundary.database :as db]
            [zanmi.config :as config]
            [zanmi.system :refer [zanmi]]
            [com.stuartsierra.component :as component]
            [duct.middleware.errors :refer [wrap-hide-errors]]
            [duct.util.runtime :refer [add-shutdown-hook]]
            [ring.middleware.ssl :refer [wrap-hsts wrap-ssl-redirect]]
            [meta-merge.core :refer [meta-merge]]))

(def ssl-config
  {:app {:middleware [[wrap-hsts wrap-ssl-redirect]]}})

(def prod-config
  {:app {:middleware [[wrap-hide-errors :internal-error]]

         :internal-error "Internal Server Error"}})

(defn- config-map [& args]
  (meta-merge config/defaults
              config/file
              config/environ
              (when-not (:skip-ssl args) ssl-config)
              prod-config))

(defn- parse-command-line-args [args]
  (into {} (map (fn [arg] (cond
                           (= arg "--skip-ssl") {:skip-ssl true}
                           (= arg "--init-db") {:init-db true}))
                args)))

(defn -main [& args]
  (let [cli    (parse-command-line-args args)
        config (config-map cli)
        system (zanmi config)]
    (if (:init-db cli)
      (do (println "Inizializing zanmi database")
          (db/initialize! (:db system)))
      (do (println "Starting zanmi http server on port" (-> system :http :port))
          (add-shutdown-hook ::stop-system #(component/stop system))
          (component/start system)))))
