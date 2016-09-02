(ns zanmi.component.logger
  (:require [zanmi.boundary.logger :as logger]
            [com.stuartsierra.component :as component]
            [ring.logger.protocols :as ring]
            [taoensso.timbre :as timbre]))

(defrecord Logger []
  component/Lifecycle
  (start [logger] logger)
  (stop [logger] logger)

  logger/Logger
  (log [logger level throwable message]
    (timbre/log* logger level throwable message))

  ring/Logger
  (add-extra-middleware [_ handler] handler)
  (log [logger level throwable message]
    (timbre/log* logger level throwable message)))

(defn logger [config]
  (map->Logger config))
