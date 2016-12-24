(ns zanmi.component.logger
  (:require [zanmi.boundary.logger :as logger]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.3rd-party.rolling
             :refer [rolling-appender]]))

(defrecord Timbre []
  logger/Logger
  (log [logger level throwable message]
    (timbre/log* logger level throwable message)))

(defn timbre [{:keys [level] :as config}]
  (let [appender-config (select-keys config [:path :pattern])]
    (map->Timbre
     {:level level
      :appenders {:rolling (rolling-appender appender-config)}})))
