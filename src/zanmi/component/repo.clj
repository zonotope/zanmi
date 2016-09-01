(ns zanmi.component.repo
  (:require [com.stuartsierra.component :as component]))

(defrecord RepoComponent [schema]
  component/Lifecycle
  (start [repo] repo)
  (stop [repo] repo))

(defn repo-component [schema]
  (->RepoComponent schema))
