(ns zanmi.util.middleware
  (:require [zanmi.component.logger :as logger]
            [ring.logger :refer [wrap-with-logger]]
            [ring.logger.protocols]
            [ring.middleware.format :refer [wrap-restful-format]]
            [taoensso.timbre :as timbre])
  (:import (zanmi.component.logger TimbreLogger)))

(extend-protocol ring.logger.protocols/Logger
  TimbreLogger
  (add-extra-middleware [_ handler] handler)
  (log [logger level throwable message]
    (timbre/log* logger level throwable message)))

(defn wrap-format [handler formats]
  (wrap-restful-format handler :formats formats))

(defn wrap-logger [handler logger]
  (wrap-with-logger handler {:logger logger}))
