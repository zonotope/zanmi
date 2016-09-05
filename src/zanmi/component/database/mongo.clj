(ns zanmi.component.database.mongo
  (:require [zanmi.boundary.database :as database]
            [com.stuartsierra.component :as component]
            [monger.core :as mongo]
            [monger.credentials :as credentials]
            [monger.command :as command])
  (:import [com.mongodb MongoOptions ServerAddress]))

(defrecord Mongo []
  component/Lifecycle
  (start [mongo]
    (if (:connection mongo)
      mongo
      (let [{:keys [db-name host password username]} mongo
            cred (credentials/create username db-name password)
            conn (mongo/connect-with-credentials host cred)]
        (assoc mongo :connection conn))))

  (stop [mongo]
    (if-let [conn (:connection mongo)]
      (do (mongo/disconnect conn)
          (dissoc mongo :connection))
      mongo)))

(defn mongo [config]
  (map->Mongo config))
