(ns zanmi.component.logger
  (:require [zanmi.boundary.logger :as logger]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.3rd-party.rolling
             :refer [rolling-appender]]))

(defrecord TimbreLogger []
  component/Lifecycle
  (start [logger] logger)
  (stop [logger] logger)

  logger/Logger
  (log [logger level throwable message]
    (timbre/log* logger level throwable message)))

(defn timbre-logger [{:keys [level path pattern] :as config}]
  (map->TimbreLogger
   {:level level
    :appenders {:rolling (rolling-appender path pattern)}}))
