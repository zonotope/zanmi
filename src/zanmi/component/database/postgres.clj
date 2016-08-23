(ns zanmi.component.database.postgres
  (:require [zanmi.boundary.database :as database]
            [honeysql.core :as sql]
            [honeysql.format :as fmt]
            [honeysql.helpers :as sql-helper :refer [defhelper delete-from
                                                     from insert-into select
                                                     sset update values where]]
            [jdbc.core :as jdbc]
            [hikari-cp.core :refer [make-datasource close-datasource]]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [clojure.string :refer [join]]
            [clojure.set :refer [rename-keys]]
            [com.stuartsierra.component :as component]))

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
    (jdbc/execute conn (str "CREATE TABLE " (name table) " ("
                            "  id UUID PRIMARY KEY NOT NULL,"
                            "  username VARCHAR(32) NOT NULL UNIQUE,"
                            "  hashed_password VARCHAR(128) NOT NULL,"
                            "  created TIMESTAMP WITHOUT TIME ZONE"
                            "          DEFAULT (now() at time zone 'utc'),"
                            "  modified TIMESTAMP WITHOUT TIME ZONE"
                            "           DEFAULT (now() at time zone 'utc')"
                            ")"))))

(defn- set-modified-trigger! [db]
  (with-open [conn (jdbc/connection (make-connection-spec db))]
    (jdbc/execute conn (str "CREATE OR REPLACE FUNCTION update_modified() "
                            "RETURNS TRIGGER AS $$ "
                            "BEGIN "
                            "  NEW.modified = (now() at time zone 'utc'); "
                            "  RETURN NEW; "
                            "END; "
                            "$$ language 'plpgsql';"))

    (jdbc/execute conn (str "CREATE TRIGGER update_" (name table) "_modified "
                            "BEFORE UPDATE ON " (name table) " "
                            "FOR EACH ROW EXECUTE PROCEDURE "
                            "  update_modified();"))))

(defn- drop-database! [{:keys [database-name] :as db}]
  (with-open [conn (jdbc/connection (postgres-db-spec db))]
    (jdbc/execute conn (str "DROP DATABASE " database-name))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; querying                                                                 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod fmt/format-clause :returning [[_ fields] _]
  (str "RETURNING " (join ", " (map fmt/to-sql fields))))

(defhelper returning [m args]
  (assoc m :returning args))

(defn- sanitize-keys [m]
  (let [keymap (into {} (map (fn [k] {k (->kebab-case-keyword k)})
                             (keys m)))]
    (rename-keys m keymap)))

(defn- query-one [db-spec statement]
  (->> statement
       (sql/format)
       (jdbc/fetch db-spec)
       (first)
       (sanitize-keys)))

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
             :spec (make-pooled-spec postgres))))

  (stop [postgres]
    (if-let [datasource (-> postgres :spec :datasource)]
      (do (close-datasource datasource)
          (dissoc postgres :spec))
      postgres))

  database/Database
  (initialize! [db]
    (create-database! db)
    (create-table! db)
    (set-modified-trigger! db))

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

(defn postgres [config]
  (-> config
      (assoc :adapter "postgresql")
      (map->Postgres)))
