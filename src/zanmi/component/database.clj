(ns zanmi.component.database
  (:require [zanmi.boundary.database :as database]
            [postgres-component.core :as postgres :refer [postgres]]
            [honeysql.core :as sql]
            [honeysql.helpers :refer [delete-from from select sset update
                                      where]]
            [clojure.string :refer [lower-case replace]]
            [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component])
  (:import [postgres_component.core Postgres]))

(let [pg-table :profiles
      pg-opts {:identifiers (fn [s] (-> s (lower-case) (replace #"_" "-")))
               :entities    (fn [s] (replace s #"-" "_"))}]

  (defn- query [db-spec q]
    (jdbc/query db-spec (sql/format q) pg-opts))

  (extend-protocol database/Database
    Postgres
    (create-database! [{db-spec :spec :as db}]
      (postgres/create-database! db))

    (drop-database! [{db-spec :spec :as db}]
      (postgres/drop-database! db))

    (create! [{db-spec :spec} attrs]
      (jdbc/insert! db-spec pg-table attrs
                    :options pg-opts))

    (update! [{db-spec :spec} username attrs]
      (query db-spec (-> (update pg-table)
                         (sset attrs)
                         (where := :username username))))

    (get [{db-spec :spec} username]
      (query db-spec (-> (select :*)
                         (from  pg-table)
                         (where := :username username))))

    (delete! [{db-spec :spec} username]
      (query db-spec (-> (delete-from pg-table)
                         (where := :username username)))))

  (defn create-table! [{db-spec :spec :as db}]
    (->> (jdbc/create-table-ddl pg-table [[:id :uuid
                                           :primary :key :not :null]

                                          [:username "varchar(32)"
                                           :not :null]

                                          [:hashed-password "char(60)"
                                           :not :null]]
                                pg-opts)
         (jdbc/db-do-commands db-spec)))

  (defn drop-table! [{db-spec :spec :as db}]
    (->> (jdbc/drop-table-ddl pg-table pg-opts)
         (jdbc/db-do-commands db-spec))))

(defn database [config]
  (postgres config))
