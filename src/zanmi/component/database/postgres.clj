(ns zanmi.component.database.postgres
  (:require [zanmi.boundary.database :as database]
            [honeysql.core :as sql]
            [honeysql.format :as fmt]
            [honeysql.helpers :as sql-helper :refer [defhelper delete-from
                                                     from insert-into select
                                                     sset update values where]]
            [jdbc.core :as jdbc]
            [hikari-cp.core :refer [make-datasource close-datasource]]
            [clojure.string :refer [join lower-case replace]]
            [com.stuartsierra.component :as component]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; db connection specs                                                      ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private table :profiles)

(defn- make-spec [postgres]
  {:datasource (make-datasource postgres)})

(defn- pg-spec [{:keys [username password server-name database-name] :as db}]
  (let [subname (str "//" server-name "/" database-name)]
    {:subprotocol "postgresql"
     :subname subname
     :user username
     :password password}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ddl                                                                      ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- create-database! [{:keys [database-name] :as db}]
  (jdbc/execute (pg-spec db) (str "CREATE DATABASE " database-name)))

(defn- drop-database! [{:keys [database-name] :as db}]
  (jdbc/execute (pg-spec db) (str "DROP DATABASE " database-name)))

(defn- create-table! [db]
  (let [db-spec (make-spec db)]
    (jdbc/execute db-spec (str "CREATE TABLE " (name table) " ("
                               "  id UUID PRIMARY KEY NOT NULL,"
                               "  username VARCHAR(32) NOT NULL UNIQUE,"
                               "  hashed_password VARCHAR(128) NOT NULL,"
                               "  created TIMESTAMP WITHOUT TIME ZONE"
                               "          DEFAULT (now() at time zone 'utc'),"
                               "  modified TIMESTAMP WITHOUT TIME ZONE"
                               "           DEFAULT (now() at time zone 'utc')"
                               ")"))))

(defn- drop-table! [db]
  (let [db-spec (make-spec db)]
    (jdbc/execute db-spec (str "DROP TABLE " (name table)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; querying                                                                 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod fmt/format-clause :returning [[_ fields] _]
  (str "RETURNING " (join ", " (map fmt/to-sql fields))))

(defhelper returning [m args]
  (assoc m :returning args))

(defn- query-one [db-spec statement]
  (->> statement
       (sql/format)
       (jdbc/fetch db-spec)
       (first)))

(defn- execute [db-spec statement]
  (->> statement
       (sql/format)
       (jdbc/execute db-spec)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; component                                                                ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord Postgres []
  component/Lifecycle
  (start [postgres]
    (if (:spec postgres)
      postgres
      (assoc postgres
             :spec (make-spec postgres))))

  (stop [postgres]
    (if-let [datasource (-> postgres :spec :datasource)]
      (do (close-datasource datasource)
          (dissoc postgres :spec))
      postgres))

  database/Database
  (initialize! [db]
    (create-database! db)
    (create-table! db))

  (destroy! [db]
    (drop-table! db)
    (drop-database! db))

  (fetch [{db-spec :spec} username]
    (query-one db-spec (-> (select :*)
                           (from table)
                           (where [:= :username username]))))

  (create! [{db-spec :spec} attrs]
    (query-one db-spec (-> (insert-into table)
                           (values [attrs])
                           (returning :*))))

  (update! [{db-spec :spec} username attrs]
    (query-one db-spec (-> (update table)
                           (sset attrs)
                           (where [:= :username username])
                           (returning :*))))

  (delete! [{db-spec :spec} username]
    (execute db-spec (-> (delete-from table)
                         (where [:= :username username])))))

(defn postgres [config]
  (-> config
      (assoc :adapter "postgresql")
      (map->Postgres)))
