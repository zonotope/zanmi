(ns dev
  (:refer-clojure :exclude [test])
  (:require [clojure.repl :refer :all]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [duct.generate :as gen]
            [meta-merge.core :refer [meta-merge]]
            [reloaded.repl :refer [system init start stop go reset]]
            [ring.middleware.stacktrace :refer [wrap-stacktrace]]
            [dev.tasks :refer :all]
            [zanmi.config :as config]
            [zanmi.system :as system]))

(def dev-config
  {:app {:middleware [wrap-stacktrace]}

   :db {:engine :postgres
        :username "zanmi"
        :password "zanmi-password"
        :host "localhost"
        :db-name "zanmi_dev"}

   :logger {:level :info
            :path "log/zanmi.log"
            :pattern :daily}

   :profile-schema {:username-length 32
                    :password-length 64
                    :password-score 3}

   :secret "nobody knows this!"

   :signer {:algorithm :rsa-pss
            :size 512
            :keypair {:public  "dev/resources/keypair/pub.pem"
                      :private "dev/resources/keypair/priv.pem"}}

   :api-key "unlock this door!"})

(def config
  (meta-merge config/defaults
              config/environ
              dev-config))

(defn new-system []
  (into (system/new-system config)
        {}))

(when (io/resource "dev/local.clj")
  (load "dev/local"))

(gen/set-ns-prefix 'zanmi)

(reloaded.repl/set-init! new-system)
