(ns zanmi.util
  (:import [com.nulabinc.zxcvbn Zxcvbn]))

(defn zxcvbn [password]
  (-> (Zxcvbn.)
      (.measure password)
      (.getScore)))
