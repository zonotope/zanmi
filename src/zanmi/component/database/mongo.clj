(ns zanmi.component.database.mongo
  (:require [com.stuartsierra.component :as component]
            [monger.core :as mongo])
  (:import [com.mongodb MongoOptions ServerAddress]))

(defrecord Mongo []
  component/Lifecycle
  (start [mongo]
    (if (:connection mongo)
      mongo
      (assoc mongo :connection (mongo/connect mongo))))

  (stop [mongo]
    (if-let [conn (:connection mongo)]
      (do (mongo/disconnect conn)
          (dissoc mongo :connection))
      mongo)))

(defn mongo [config]
  (map->Mongo config))
