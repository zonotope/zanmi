(defproject zanmi "0.1.0-SNAPSHOT"
  :description "HTTP authentication service built on buddy"

  :url "https://github.com/zonotope/zanmi"

  :min-lein-version "2.0.0"

  :dependencies [[org.clojure/clojure "1.9.0-alpha11"]

                 [com.stuartsierra/component "0.3.1"]
                 [compojure "1.5.0"]
                 [duct "0.7.0"]

                 [buddy/buddy-hashers "0.14.0"]
                 [buddy/buddy-sign "1.1.0"]
                 [danlentz/clj-uuid "0.1.6"]
                 [zxcvbn-clj "0.1.0-SNAPSHOT"]

                 [honeysql "0.8.0"]
                 [funcool/clojure.jdbc "0.9.0"]

                 [ring "1.4.0"]
                 [ring/ring-defaults "0.2.0"]
                 [ring-middleware-format "0.7.0"]
                 [ring-jetty-component "0.3.1"]

                 [bouncer "1.0.0"]
                 [camel-snake-kebab "0.4.0"]
                 [environ "1.1.0"]
                 [meta-merge "0.1.1"]]

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

   :project/dev   {:dependencies [[org.clojure/test.check "0.9.0"]
                                  [duct/generate "0.7.0"]
                                  [reloaded.repl "0.2.2"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [org.clojure/tools.nrepl "0.2.12"]
                                  [eftest "0.1.1"]
                                  [com.gearswithingears/shrubbery "0.3.1"]
                                  [kerodon "0.7.0"]]
                   :source-paths ["dev"]
                   :repl-options {:init-ns user}
                   :env {:port "3000"
                         :database-host "localhost"
                         :database-name "zanmi_dev"
                         :database-password "zanmi-password"}}

   :project/test  {}})
