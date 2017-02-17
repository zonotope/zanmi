(ns dev
  (:refer-clojure :exclude [test])
  (:require [zanmi.config :as config]
            [zanmi.system :as system]
            [clojure.repl :refer :all]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [duct.generate :as gen]
            [meta-merge.core :refer [meta-merge]]
            [reloaded.repl :refer [system init start stop go reset]]
            [ring.middleware.stacktrace :refer [wrap-stacktrace]]
            [dev.tasks :refer :all]))

(def dev-config {:app {:allowed-origins ["http://localhost:3000"]
                       :middleware [wrap-stacktrace]}})

(def config
  (meta-merge config/defaults
              config/file
              config/environ
              dev-config))

(defn zanmi []
  (into (system/zanmi config)
        {}))

(when (io/resource "dev/local.clj")
  (load "dev/local"))

(gen/set-ns-prefix 'zanmi)

(reloaded.repl/set-init! zanmi)
