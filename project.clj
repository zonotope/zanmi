(defproject zanmi "0.1.0-alpha0"
  :description "Identity http service"

  :url "https://github.com/zonotope/zanmi"

  :min-lein-version "2.0.0"

  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.clojure/data.codec "0.1.0"]

                 [com.stuartsierra/component "0.3.1"]
                 [compojure "1.5.0"]
                 [duct "0.7.0"]

                 [buddy/buddy-auth "1.3.0"]
                 [buddy/buddy-hashers "0.14.0"]
                 [buddy/buddy-sign "1.1.0"]
                 [zxcvbn-clj "0.1.0-SNAPSHOT"]

                 [funcool/clojure.jdbc "0.9.0"]
                 [hikari-cp "1.7.3"]
                 [honeysql "0.8.0"]
                 [org.postgresql/postgresql "9.4.1208"]

                 [com.novemberain/monger "3.0.2"]

                 [ring "1.4.0"]
                 [ring/ring-defaults "0.2.0"]
                 [ring/ring-ssl "0.2.1"]
                 [org.immutant/web "2.1.6"]
                 [ring-logger "0.7.6"]
                 [ring-middleware-format "0.7.0"]

                 [bouncer "1.0.0"]
                 [camel-snake-kebab "0.4.0"]
                 [clj-time "0.12.0"]
                 [environ "1.1.0"]
                 [meta-merge "0.1.1"]
                 [com.taoensso/timbre "4.7.4"]]

  :plugins [[lein-environ "1.0.3"]]

  :main ^:skip-aot zanmi.main

  :target-path "target/%s/"

  :aliases {"run-task" ["with-profile" "+repl" "run" "-m"]
            "setup"    ["run-task" "dev.tasks/setup"]}

  :profiles
  {:dev  [:project/dev  :profiles/dev]

   :test [:project/test :profiles/test]

   :uberjar {:aot :all}

   :profiles/dev  {}

   :profiles/test {}

   :project/dev   {:dependencies [[com.gearswithingears/shrubbery "0.4.1"]
                                  [duct/generate "0.7.0"]
                                  [eftest "0.1.1"]
                                  [org.clojure/test.check "0.9.0"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [org.clojure/tools.nrepl "0.2.12"]
                                  [kerodon "0.7.0"]
                                  [reloaded.repl "0.2.2"]]

                   :source-paths ["dev"]

                   :repl-options {:init-ns user}

                   :env {:zanmi-config "resources/zanmi/config.edn"}}

   :project/test  {}})
