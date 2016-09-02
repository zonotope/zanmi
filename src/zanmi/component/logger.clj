(ns zanmi.component.logger
  (:require [zanmi.boundary.logger :as logger]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]))

(defrecord Logger []
  component/Lifecycle
  (start [logger] logger)
  (stop [logger] logger)

  logger/Logger
  (log [logger level message]
    (timbre/log* logger level message)))

(defn logger [config]
  (map->Logger config))
