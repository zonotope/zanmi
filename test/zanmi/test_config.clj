(ns zanmi.test-config)

(def config
  {:db {:engine :postgres
        :username "zanmi"
        :password "zanmi-password"
        :host "localhost"
        :db-name "zanmi_test"}

   :profile-schema {:username-length 32
                    :password-length 64
                    :password-score 1}

   :secret "nobody knows this!"})
