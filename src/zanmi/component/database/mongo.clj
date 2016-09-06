(ns zanmi.component.database.mongo
  (:require [zanmi.boundary.database :as database]
            [zanmi.util.time :as time]
            [camel-snake-kebab.core :refer [->camelCaseKeyword
                                            ->kebab-case-keyword
                                            ->snake_case_keyword]]
            [clojure.set :refer [rename-keys]]
            [com.stuartsierra.component :as component]
            [monger.core :as mongo]
            [monger.collection :as collection]
            [monger.credentials :as credentials]
            [monger.operators :refer [$set]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; key sanitization                                                         ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- transform-keys [m f & overrides]
  (let [keymap (-> (into {} (map #(vector % (f %))) (keys m))
                   (merge (apply array-map overrides)))]
    (rename-keys m keymap)))

(defn- map->doc [m]
  (transform-keys m ->snake_case_keyword :id :_id))

(defn- doc->map [d]
  (transform-keys d ->kebab-case-keyword))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; query utils                                                              ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private collection "profiles")

(defn- execute [{db :db} cmd & args]
  (let [mongo-args (->> args
                        (map map->doc)
                        (cons collection)
                        (cons db))]
    (apply cmd mongo-args)))

(defn- execute-map [mongo cmd & args]
  (doc->map (execute mongo cmd args)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; component                                                                ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord Mongo []
  component/Lifecycle
  (start [mongo]
    (if (:connection mongo)
      mongo
      (let [{:keys [db-name host password username]} mongo
            cred (credentials/create username db-name password)
            conn (mongo/connect-with-credentials host cred)
            db   (mongo/get-db conn db-name)]
        (assoc mongo
               :connection conn
               :db db))))

  (stop [mongo]
    (if-let [conn (:connection mongo)]
      (do (mongo/disconnect conn)
          (dissoc mongo :connection :db))
      mongo))

  database/Database
  (initialize! [{db :db}]
    (collection/create db collection {:capped false})
    (collection/ensure-index db collection {:username 1} {:unique true}))

  (destroy! [{db :db}]
    (mongo/command db {:dropDatabase 1}))

  (fetch [{db :db} username]
    (doc->map (collection/find-one-as-map db collection {:username username})))

  (create! [{db :db} attrs]
    (doc->map (collection/insert-and-return db collection (map->doc attrs))))

  (update! [{db :db} username attrs]
    (doc->map (collection/find-and-modify db collection
                                          {:username username}
                                          {$set (map->doc attrs)}
                                          {:return-new true})))

  (delete! [{db :db} username]
    (collection/remove db collection {:username username})))

(defn mongo [config]
  (map->Mongo config))
