(ns zanmi.util.time
  (:require [clj-time.core :as time]))

(defn now []
  (.toDate (time/now)))

(defn in-hours [hours]
  (.toDate (time/plus (time/now) (time/hours hours))))
