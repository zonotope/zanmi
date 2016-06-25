(ns dev.tasks
  (:refer-clojure :exclude [test])
  (:require [duct.generate :as gen]
            [duct.component.ragtime :as ragtime]
            [eftest.runner :as eftest]
            [reloaded.repl :refer [system]]))

(defn setup []
  (gen/locals))

(defn test []
  (eftest/run-tests (eftest/find-tests "test") {:multithread? false}))

(defn migrate []
  (-> system :ragtime ragtime/reload ragtime/migrate))

(defn rollback
  ([]  (rollback 1))
  ([x] (-> system :ragtime ragtime/reload (ragtime/rollback x))))
