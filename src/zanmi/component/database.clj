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
               :entities (fn [s] (replace s #"-" "_"))}]

  (defn- build-pg-spec [{:keys [username password server-name database-name]
                         :as db}]
    (let [subname (str "//" server-name "/" database-name)]
      {:subprotocol "postgresql"
       :subname subname
       :user username
       :password password}))

  (defn- create-table! [db]
    (let [db-spec (build-pg-spec db)]
      (->> (jdbc/create-table-ddl pg-table [[:id :uuid
                                             :primary :key :not :null]

                                            [:username "varchar(32)"
                                             :not :null :unique]

                                            [:hashed-password "varchar(128)"
                                             :not :null]]
                                  pg-opts)
           (jdbc/db-do-commands db-spec))))

  (defn- drop-table! [db]
    (let [db-spec (build-pg-spec db)]
      (->> (jdbc/drop-table-ddl pg-table pg-opts)
           (jdbc/db-do-commands db-spec))))

  (defn- by-username [username]
    ["username = ?" username])

  (defn- pg-returning-all [sql-stmt]
    (update-in sql-stmt [0] #(str % " RETURNING *")))

  (defn- pg-update-sql [username attrs]
    (-> (update pg-table)
        (sset attrs)
        (where [:= :username username])
        (sql/format)
        (pg-returning-all)))

  (extend-protocol database/Database
    Postgres
    (initialize! [db]
      (postgres/create-database! db)
      (create-table! db))

    (destroy! [db]
      (drop-table! db)
      (postgres/drop-database! db))

    (fetch [{db-spec :spec} username]
      (jdbc/get-by-id db-spec pg-table username :username pg-opts))

    (create! [{db-spec :spec} attrs]
      (jdbc/insert! db-spec pg-table attrs pg-opts))

    (update! [{db-spec :spec} username attrs]
      (first (jdbc/query db-spec (pg-update-sql username attrs) pg-opts)))

    (delete! [{db-spec :spec} username]
      (jdbc/delete! db-spec pg-table (by-username username) pg-opts))))

(defn database [config]
  (postgres config))
