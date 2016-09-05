(ns zanmi.component.mongo
  (:require [monger.core :as mongo])
  (:import [com.mongodb MongoOptions ServerAddress]))

(defrecord Mongo []
  )

(defn mongo [config]
  (map->Mongo config))
