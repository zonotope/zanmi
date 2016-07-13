(ns zanmi.boundary.database)

(defprotocol Database
  "Interact with a database"
  (create-database! [db-config] "create the database specified by `db-config`")
  (drop-database! [db-config] "drop the database specified by `db-config`"))
