(ns zanmi.boundary.logger)

(defprotocol Logger
  "Log messages"
  (log [logger level throwable message]
    "Log `message` to `logger` along with the trace from `throwable`"))

(defn trace [logger message]
  (log logger :trace nil message))

(defn debug [logger message]
  (log logger :debug nil message))

(defn info [logger message]
  (log logger :info nil message))

(defn warn [logger message]
  (log logger :warn nil message))

(defn error
  ([logger message]
   (log logger :error nil message))
  ([logger throwable message]
   (log logger :error throwable message)))

(defn fatal
  ([logger message]
   (log logger :fatal nil message))
  ([logger throwable message]
   (log logger :fatal throwable message)))

(defn report [logger message]
  (log logger :report nil message))
