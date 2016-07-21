(ns zanmi.component.database
  (:require [zanmi.boundary.database :as database]
            [postgres-component.core :as postgres :refer [postgres]]
            [honeysql.core :as sql]
            [honeysql.helpers :refer [delete-from from select sset update
                                      where]]
            [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component])
  (:import [postgres_component.core Postgres]))


(let [pg-table :profiles]
  (extend-protocol database/Database
    Postgres
    (create-database! [db]
      (postgres/create-database! db))

    (drop-database! [db]
      (postgres/drop-database! db))

    (create! [{db-spec :spec} attrs]
      (jdbc/insert! pg-table db-spec attrs))

    (update! [{db-spec :spec} username attrs]
      (jdbc/query db-spec (-> (update pg-table)
                              (sset attrs)
                              (where := :username username)
                              (sql/format))))

    (get [{db-spec :spec} username]
      (jdbc/query db-spec (-> (select :*)
                              (from  pg-table)
                              (where := :username username)
                              (sql/format))))

    (delete! [{db-spec :spec} username]
      (jdbc/query db-spec (-> (delete-from pg-table)
                              (where := :username username)
                              (sql/format))))))

(defn database [config]
  (postgres config))
