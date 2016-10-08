(ns zanmi.data.profile-test
  (:require [zanmi.data.profile :refer :all]
            [clojure.test :refer :all]))

(deftest test-authenticate
  (let [profile (hash-password (with-id {:username "test-user"
                                         :password "correct"}))]
    (testing "authenticate"
      (testing "with the correct password"
        (is (= (authenticate profile "correct")
               profile)
            "returns the profile"))

      (testing "with the wrong password"
        (is (nil? (authenticate profile "wrong"))
            "returns nil")))))

(deftest test-create
  )

(deftest test-update)
