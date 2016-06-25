(ns zanmi.endpoint.example
  (:require [compojure.core :refer :all]))

(defn example-endpoint [{{db :spec} :db}]
  (context "/example" []
    (GET "/" []
      "This is an example endpoint")))
