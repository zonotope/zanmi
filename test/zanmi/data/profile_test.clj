(ns zanmi.data.profile-test
  (:require [zanmi.data.profile :refer :all]
            [clojure.test :refer :all]))

(deftest test-authenticate
  (let [profile (build {:username "test-user", :password "correct"})]
    (is (= (authenticate profile "correct") profile))
    (is (nil? (authenticate profile "wrong")))))
