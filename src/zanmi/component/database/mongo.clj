(ns zanmi.component.database.mongo
  (:require [zanmi.boundary.database :as database]
            [camel-snake-kebab.core :refer [->camelCaseKeyword
                                            ->kebab-case-keyword
                                            ->snake_case_keyword]]
            [com.stuartsierra.component :as component]
            [monger.core :as mongo]
            [monger.collection :as collection]
            [monger.credentials :as credentials]
            [monger.operators :refer [$set]]))

(def ^:private collection "profiles")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; key sanitization                                                         ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- transform-keys
  ([m f]
   (transform-keys m f {}))
  ([m f overrides]
   (into {} (for [[k v] m]
              (let [new-key (if-let [override (k overrides)] override (f k))]
                [new-key v])))))

(defn- map->doc [m]
  (transform-keys m ->snake_case_keyword {:id :_id}))

(defn- doc->map [d]
  (transform-keys d ->kebab-case-keyword))

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
               :database db))))

  (stop [mongo]
    (if-let [conn (:connection mongo)]
      (do (mongo/disconnect conn)
          (dissoc mongo :connection :database))
      mongo))

  database/Database
  (initialize! [{db :database}]
    (collection/create db collection {:capped false})
    (collection/ensure-index db collection {:username 1} {:unique true}))

  (destroy! [{db :database}]
    (mongo/command db {:dropDatabase 1}))

  (fetch [{db :database} username]
    (let [attr {:username username}]
      (doc->map (collection/find-one-as-map db collection attr))))

  (create! [{db :database} attrs]
    (let [attrs-doc (map->doc attrs)]
      (doc->map (collection/insert-and-return db collection attrs-doc))))

  (update! [{db :database} username attrs]
    (let [attr-doc (map->doc attrs)
          query (map->doc {:username username})]
      (doc->map (collection/find-and-modify db collection
                                            {:username username}
                                            {$set attr-doc}
                                            {:return-new true}))))

  (delete! [{db :database} username]
    (collection/remove db collection {:username username})))

(defn mongo [config]
  (map->Mongo config))
