(ns zanmi.util.codec
  (:require [clojure.data.codec.base64 :as base64]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; string<->byte conversion                                                 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- bytes->string [bytes]
  (String. bytes "UTF-8"))

(defn- string->bytes [string]
  (.getBytes string "UTF-8"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; base64                                                                   ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- base64-transform [string transform-fn opts]
  (let [byte-array (string->bytes string)
        transformed (transform-fn byte-array)]
    (if (:bytes opts)
      transformed
      (bytes->string transformed))))

(defn base64-encode [string & opts]
  (base64-transform string base64/encode opts))

(defn base64-decode [string & opts]
  (base64-transform string base64/decode opts))
