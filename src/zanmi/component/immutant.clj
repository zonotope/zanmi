(ns zanmi.component.immutant
  (:require [com.stuartsierra.component :as component]
            [immutant.web :as web]))

(defrecord ImmutantWeb []
  component/Lifecycle
  (start [immutant]
    (if-not (:server immutant)
      (let [host (or (:host immutant)
                     "0.0.0.0")
            port   (:port immutant)
            config {:host host :port port}
            handler (:app immutant)
            server (do (-> (str "Starting web server. Listening on host: %s "
                                "and port: %d")
                           (format host port)
                           (println))
                       (web/run (:handler handler) config))]
        (assoc immutant
               :server server
               :host host))
      immutant))

  (stop [immutant]
    (if-let [server (:server immutant)]
      (do (-> (str "Stopping web server on host: %s and port: %d")
              (format (:host immutant) (:port immutant))
              (println))
          (web/stop server)
          (dissoc immutant :server))
      immutant)))

(defn immutant-web-server [config]
  (map->ImmutantWeb config))
