(ns zanmi.view.profile-view-test
  (:require [zanmi.boundary.signer :as signer]
            [zanmi.component.signer.sha :refer [sha-signer]]
            [zanmi.data.profile :refer [create]]
            [zanmi.test-config :refer [config]]
            [zanmi.util.time :as time]
            [zanmi.view.profile-view :refer :all]
            [clojure.test :refer :all]))

(deftest render-token-test
  (testing "render-token"
    (let [now (time/now)
          profile {:username "tester", :hashed-password "a long hash",
                   :id (java.util.UUID/randomUUID), :created now, :modified now}
          secret "nobody knows this!"
          signer (sha-signer {:secret secret, :size 256})
          subject (:token (render-token profile signer))]
      (is (not (nil? subject))
          "renders the token")

      (testing "signs the data"
        (let [unsigned (signer/unsign signer subject)]
          (is (not (nil? (:username unsigned)))
              "includes the username")

          (is (not (nil? (:id unsigned)))
              "includes the id")

          (is (not (nil? (:modified unsigned)))
              "includes the modified time")

          (is (nil? (:hashed-password unsigned))
              "does not include the hashed password"))))))

(deftest render-message-test
  (testing "render-message"
    (let [msg "important message"
          subject (render-message msg)]
      (is (= (:message subject) msg)
          "renders the message"))))

(deftest render-error-test
  (testing "render-error"
    (let [e "bad news"
          subject (render-error e)]
      (is (= (:error subject) e)
          "renders the error"))))
