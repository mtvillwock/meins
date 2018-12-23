(ns meo.jvm.log
  (:require [taoensso.timbre :as timbre :refer [info]]
            [taoensso.timbre.appenders.3rd-party.rolling :as tr]
            [taoensso.encore :as enc]
            [clojure.tools.logging :as log]))

(defn ns-filter
  "From: https://github.com/yonatane/timbre-ns-pattern-level"
  [fltr]
  (-> fltr enc/compile-ns-filter taoensso.encore/memoize_))

(def namespace-log-levels
  {:all :info})

(defn middleware
  "From: https://github.com/yonatane/timbre-ns-pattern-level"
  [ns-patterns]
  (fn log-by-ns-pattern [{:keys [?ns-str config level] :as opts}]
    (let [namesp (or (some->> ns-patterns
                              keys
                              (filter #(and (string? %)
                                            ((ns-filter %) ?ns-str)))
                              not-empty
                              (apply max-key count))
                     :all)
          log-level (get ns-patterns namesp (get config :level))]
      (when (and (taoensso.timbre/may-log? log-level namesp)
                 (taoensso.timbre/level>= level log-level))
        opts))))

(def filename (if-let [logfile (get (System/getenv) "LOG_FILE")]
                logfile
                "./log/meo.log"))

(def custom-log-appender
  {:enabled?  true
   :async?    true
   :min-level nil
   :output-fn :inherit
   :fn        (fn [data]
                (let [{:keys [output_ level]} data
                      formatted-output-str (force output_)]
                  (case level
                    :error (log/error formatted-output-str)
                    :debug (log/debug formatted-output-str)
                    :trace (log/trace formatted-output-str)
                    :warn (log/warn formatted-output-str)
                    (log/info formatted-output-str))))})

(timbre/set-config!
  {:level          :info
   :timestamp-opts {:pattern "yyyy-MM-dd HH:mm:ss.SSS"}
   :appenders      {;:rolling (tr/rolling-appender {:path filename})
                    :tools-logging custom-log-appender}})
