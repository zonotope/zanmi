(ns zanmi.util.codec-test
  (:require [zanmi.util.codec :refer :all]
            [clojure.test :refer :all]))

(let [test-string "this is a test"
      encoded-value "dGhpcyBpcyBhIHRlc3Q="]

  (deftest base64-encode-test
    (testing "base64-encode"
      (let [subject (base64-encode test-string)]
        (is (= subject encoded-value)
            "encodes the string")

        (is (string? subject)
            "returns a string"))))

  (deftest base64-decode-test
    (testing "base64-decode"
      (let [subject (base64-decode encoded-value)]
        (is (= subject test-string)
            "decodes the value")

        (is (string? subject)
            "returns a string")))))
