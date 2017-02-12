(ns zanmi.middleware.cors
  (:require [ring.middleware.cors :as cors]))

(defn wrap-cors [handler allowed-origins]
  (let [allowed-origin-patterns (map re-pattern allowed-origins)]
    (cors/wrap-cors handler
                    :access-control-allow-origin allowed-origin-patterns
                    :access-control-allow-methods [:post :put :delete])))
