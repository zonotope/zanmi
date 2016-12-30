(ns zanmi.middleware.logger
  (:require [zanmi.component.timbre :as logger]
            [ring.logger :refer [wrap-with-logger]]
            [ring.logger.protocols]
            [taoensso.timbre :as timbre])
  (:import (zanmi.component.timbre Timbre)))

(extend-protocol ring.logger.protocols/Logger
  Timbre
  (add-extra-middleware [_ handler] handler)
  (log [logger level throwable message]
    (timbre/log* logger level throwable message)))

(defn wrap-logger [handler logger]
  (wrap-with-logger handler {:logger logger}))
