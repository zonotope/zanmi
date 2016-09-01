(ns zanmi.component.data-repo
  (:require [com.stuartsierra.component :as component]))

(defrecord DataRepoComponent [schema]
  component/Lifecycle
  (start [data-repo] data-repo)
  (stop [data-repo] data-repo))

(defn data-repo-component [schema]
  (->DataRepoComponent schema))
