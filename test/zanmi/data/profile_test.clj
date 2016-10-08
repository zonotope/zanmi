(ns zanmi.data.profile-test
  (:require [zanmi.data.profile :refer :all]
            [zanmi.test-config :refer [config]]
            [clojure.test :refer :all]))

(def schema (:schema (profile-repo (:profile-repo config))))

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
  (testing "create"
    (testing "with valid attributes"
      (let [profile (:ok (create schema {:username "tester"
                                         :password "this is only a test"}))]
        (is (not (nil? profile))
            "returns the profile")

        (is (not (nil? (:id profile)))
            "includes an id")

        (is (nil? (:password profile))
            "doesn't include the raw password")

        (is (not (nil? :hashed-password))
            "includes the hashed password")))

    (testing "with invalid attributes")))

(deftest test-update
  (testing "update"
    (testing "with a valid new password")
    (testing "with an invalid new password")))
