(ns zanmi.util.response
  (:require [zanmi.view.profile-view :refer [render-error]]
            [ring.util.response :as response :refer [response]]))

(defn error [msg status]
  (-> (response (render-error msg))
      (assoc :status status)))
