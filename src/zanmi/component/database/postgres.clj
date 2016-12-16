(ns zanmi.component.database.postgres
  (:require [zanmi.boundary.database :as database]
            [zanmi.config :as config]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clojure.string :refer [join]]
            [clojure.set :refer [rename-keys]]
            [com.stuartsierra.component :as component]
            [hikari-cp.core :refer [make-datasource close-datasource]]
            [honeysql.core :as sql]
            [honeysql.format :as sql.fmt]
            [honeysql.helpers :as sql-helper :refer [defhelper delete-from
                                                     from insert-into select
                                                     sset update values where]]
            [jdbc.core :as jdbc]
            [jdbc.proto]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; db connection specs                                                      ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- make-pooled-spec [postgres]
  {:datasource (make-datasource postgres)})

(defn- make-connection-spec [{:keys [username password server-name
                                     database-name]
                              :as db}]
  (let [subname (str "//" server-name "/" database-name)]
    {:subprotocol "postgresql"
     :subname subname
     :user username
     :password password}))

(defn- postgres-db-spec [db]
  (make-connection-spec (assoc db :database-name "postgres")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ddl                                                                      ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private table :profiles)

(defn- create-database! [{:keys [database-name] :as db}]
  (with-open [conn (jdbc/connection (postgres-db-spec db))]
    (jdbc/execute conn (str "CREATE DATABASE " database-name))))

(defn- create-table! [db]
  (with-open [conn (jdbc/connection (make-connection-spec db))]
    (let [length 32]
      (jdbc/execute conn (str "CREATE TABLE " (name table) " ("
                              "  id UUID PRIMARY KEY NOT NULL,"
                              "  username VARCHAR(" length ") NOT NULL UNIQUE,"
                              "  hashed_password VARCHAR(128) NOT NULL,"
                              "  created TIMESTAMP NOT NULL,"
                              "  modified TIMESTAMP NOT NULL"
                              ")")))))

(defn- drop-database! [{:keys [database-name] :as db}]
  (with-open [conn (jdbc/connection (postgres-db-spec db))]
    (jdbc/execute conn (str "DROP DATABASE " database-name))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; sql time conversion                                                      ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-protocol jdbc.proto/ISQLType
  java.util.Date
  (as-sql-type [date conn]
    (java.sql.Timestamp. (.getTime date)))

  (set-stmt-parameter! [date conn stmt index]
    (.setObject stmt index (jdbc.proto/as-sql-type date conn))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; querying                                                                 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod sql.fmt/format-clause :returning [[_ fields] _]
  (str "RETURNING " (join ", " (map sql.fmt/to-sql fields))))

(defhelper returning [m args]
  (assoc m :returning args))

(defn- sanitize-keys [m]
  (let [keymap (into {} (map (fn [k] {k (->kebab-case-keyword k)})
                             (keys m)))]
    (rename-keys m keymap)))

(defn- query-one [db-spec statement]
  (with-open [conn (jdbc/connection db-spec)]
    (->> statement
         (sql/format)
         (jdbc/fetch conn)
         (first)
         (sanitize-keys))))

(defn- execute [db-spec statement]
  (with-open [conn (jdbc/connection db-spec)]
    (->> statement
         (sql/format)
         (jdbc/execute conn))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; component                                                                ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord Postgres []
  component/Lifecycle
  (start [postgres]
    (if (:spec postgres)
      postgres
      (assoc postgres
             :spec (make-pooled-spec postgres))))

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

(defn postgres [{:keys [db-name host] :as config}]
  (-> config
      (dissoc :engine :host :db-name)
      (assoc :adapter "postgresql"
             :server-name host
             :database-name db-name)
      (map->Postgres)))
