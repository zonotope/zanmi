(ns zanmi.component.database
  (:require [zanmi.component.database.postgres :refer [postgres]]))

(defn database [config]
  (postgres config))
