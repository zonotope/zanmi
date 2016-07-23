(ns zanmi.boundary.database)

(defprotocol Database
  "Interact with a database"
  (create-database! [db]
    "create the database specified by `db`")
  (drop-database! [db]
    "drop the database specified by `db`")
  (create! [db attrs]
    "save the new profile for `username` with `password` in `db`")
  (get [db username]
    "get the profile for `username` from `db`")
  (update! [db username new-password]
    "update the saved profile for `username` with `new-password` in `db`")
  (delete! [db username]
    "remove the profile for `username` from `db`"))
