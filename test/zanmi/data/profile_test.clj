(ns zanmi.data.profile-test
  (:require [zanmi.data.profile :as profile]
            [clojure.test :refer :all]))

(deftest test-authenticate
  (let [profile (profile/build {:username "test-user", :password "correct"})]
    (is (= (profile/authenticate profile "correct") profile))
    (is (nil? (profile/authenticate profile "wrong")))))
