(ns zanmi.boundary.database)

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
  (update! [db username new-password]
    "update the saved profile for `username` with `new-password` in `db`")
  (delete! [db username]
    "remove the profile for `username` from `db`"))
