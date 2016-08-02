(ns dev.tasks
  (:refer-clojure :exclude [test])
  (:require [duct.generate :as gen]
            [eftest.runner :as eftest]
            [reloaded.repl :refer [system]]))

(defn setup []
  (gen/locals))

(defn test []
  (eftest/run-tests (eftest/find-tests "test") {:multithread? false}))
