(ns zanmi.component.keypair
  (:require [com.stuartsierra.component :as component]
            [buddy.core.keys :as keys]))

(defrecord KeyPair [public-path private-path]
  component/Lifecycle
  (start [keypair]
    (if-not (and (:public keypair) (:private keypair))
      (let [public (keys/public-key public-path)
            private (keys/private-key private-path)]
        (assoc keypair :public public :private private))
      keypair))

  (stop [keypair]
    (dissoc keypair :public :private)))

(defn keypair [{:keys [public private] :as config}]
  (->KeyPair public private))
