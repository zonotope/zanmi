(ns zanmi.component.schema
  (:require [com.stuartsierra.component :as component]))

(defrecord SchemaComponent [schema]
  component/Lifecycle
  (start [schema] schema)
  (stop [schema] schema))

(defn schema-component [schema]
  (->SchemaComponent schema))
