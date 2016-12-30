(ns zanmi.middleware.format
  (:require [ring.middleware.format :refer [wrap-restful-format]]))

(defn wrap-format [handler formats]
  (wrap-restful-format handler :formats formats))
