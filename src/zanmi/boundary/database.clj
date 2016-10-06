(ns zanmi.boundary.database
  (require [clojure.core.match :refer [match]]))

(defprotocol Database
  "Interact with a database"
  (initialize! [db]
    "create the database specified by `db`")
  (destroy! [db]
    "drop the database specified by `db`")
  (create! [db attrs]
    "save the new profile with `attrs` in `db`")
  (fetch [db username]
    "fetch the profile for `username` from `db`")
  (update! [db username attrs]
    "update the saved profile for `username` with `attrs` in `db`")
  (delete! [db username]
    "remove the profile for `username` from `db`"))

(defn save! [db validated]
  (match validated
    [:ok new-profile] (try [:ok (create! db new-profile)]
                           (catch Exception e [:error (.getMessage e)]))
    :else validated))

(defn set! [db username validated]
  (match validated
    [:ok attrs] (try [:ok (update! db username attrs)]
                     (catch Exception e [:error (.getMessage e)]))
    :else validated))
