(ns zanmi.component.logger
  (:require [com.stuartsierra.component :as component]))

(defrecord Logger []
  component/Lifecycle
  (start [logger] logger)
  (stop [logger] logger))

(defn logger [config]
  (map->Logger config))
