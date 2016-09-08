(ns zanmi.component.database
  (:require [zanmi.component.database.mongo :refer [mongo]]
            [zanmi.component.database.postgres :refer [postgres]]))

(defn database [config]
  (case (:engine config)
    :postgres (postgres config)
    :mongo    (mongo config)))
