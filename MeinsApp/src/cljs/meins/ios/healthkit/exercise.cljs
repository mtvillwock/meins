(ns meins.ios.healthkit.exercise
  (:require [meins.ios.healthkit.common :as hc]
            ["@matthiasn/rn-apple-healthkit" :as hk]
            ["moment" :as moment]
            [matthiasn.systems-toolbox.component :as st]))

(defn res-cb [tag k offset put-fn err res]
  (when err (js/console.error err))
  (doseq [sample (js->clj res)]
    (let [v (get-in sample ["value"])
          end-date (get-in sample ["endDate"])
          end-ts (.valueOf (moment end-date))
          v (int (/ v 60))
          entry {:timestamp     (- end-ts offset)
                 :md            (str v " minutes " tag)
                 :tags          #{tag}
                 :perm_tags     #{tag}
                 :hidden        true
                 :sample        sample
                 :custom_fields {tag {k v}}}]
      (put-fn (with-meta [:entry/update entry] {:silent true}))
      (put-fn [:entry/persist entry]))))

(defn get-exercise [{:keys [msg-payload put-fn current-state]}]
  (let [start (or (:last-read-exercise current-state)
                  (hc/days-ago (:n msg-payload)))
        opts (clj->js {:startDate start})
        now-dt (hc/date-from-ts (st/now))
        exercise-cb (partial res-cb "#exercise" :minutes 400 put-fn)
        init-cb (fn [_err _res]
                  (.getBasalEnergyBurned hk opts exercise-cb))
        new-state (assoc current-state :last-read-exercise now-dt)]
    (.initHealthKit hk hc/hk-opts init-cb)
    {:new-state new-state}))
