(ns zanmi.component.repo
  (:require [com.stuartsierra.component :as component]))

(defrecord Repo [schema]
  component/Lifecycle
  (start [repo] repo)
  (stop [repo] repo))

(defn repo [schema]
  (->Repo schema))
