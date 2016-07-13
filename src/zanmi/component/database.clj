(ns zanmi.component.database
  (:require [zanmi.boundary.database :as database]
            [postgres-component.core :as postgres :refer [postgres]]
            [hikari-cp.core :refer [make-datasource close-datasource]]
            [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component])
  (:import [postgres_component.core Postgres]))

(extend-protocol database/Database
  Postgres
  (create-database! [db]
    (postgres/create-database! db))

  (drop-database! [db]
    (postgres/drop-database! db)))

(defn database [config]
  (postgres config))
