(ns zanmi.boundary.database)

(defprotocol Database
  "Interact with a database"
  (create-database! [db] "create the database specified by `db`")
  (drop-database!   [db] "drop the database specified by `db`")

  (create! [db attrs]    "save the new profile described in `attrs`")
  (get     [db username] "get the profile for `username`")
  (update! [db attrs]    "update the saved profile described in `attrs`")
  (delete! [db username] "remove the profile record for `username`"))
