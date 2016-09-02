(ns zanmi.boundary.logger)

(defprotocol Logger
  "Log messages"
  (log [logger level message] "Log `message` to `logger`"))

(defn trace [logger message]
  (log logger :trace message))

(defn debug [logger message]
  (log logger :debug message))

(defn info [logger message]
  (log logger :info message))

(defn warn [logger message]
  (log logger :warn message))

(defn error [logger message]
  (log logger :error message))

(defn fatal [logger message]
  (log logger :fatal message))

(defn report [logger message]
  (log logger :report message))
