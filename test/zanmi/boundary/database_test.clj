(ns zanmi.boundary.database-test
  (require [zanmi.boundary.database :as db]
           [zanmi.util.time :as time]
           [clojure.test :refer :all]
           [clj-uuid :as uuid]
           [shrubbery.core :as shrubbery :refer [mock received?]]))

(let [now (time/now)
      profile {:id (uuid/null), :username "tester",
               :hashed-password "corned beef", :created now, :modified now}]

  (deftest save!-test
    (let [test-db (mock db/Database {:create! profile})]
      (testing "save!"
        (is (not (received? test-db db/create!))
            "starts with a clean db mock")

        (is (= (db/save! test-db {:ok profile})
               {:ok profile})
            "saves to the database")

        (is (received? test-db db/create!)
            "calls db/create!"))))

  (deftest set!-test
    (let [test-db (mock db/Database {:update! profile})]
      (testing "set!"
        (is (not (received? test-db db/update!))
            "starts with a clean db mock")

        (is (= (db/set! test-db "tester" {:ok profile})
               {:ok profile})
            "sets attributes in the database")

        (is (received? test-db db/update!)
            "calls db/update!")))))
