(ns zanmi.util.time
  (:require [clj-time.core :as time]))

(defn now []
  (.toDate (time/now)))
