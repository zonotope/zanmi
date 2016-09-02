(ns zanmi.util.middleware
  (:require [ring.logger :refer [wrap-with-logger]]
            [ring.middleware.format :refer [wrap-restful-format]]))

(defn wrap-format [handler formats]
  (wrap-restful-format handler :formats formats))

(defn wrap-logger [handler logger]
  (wrap-with-logger handler {:logger logger}))
