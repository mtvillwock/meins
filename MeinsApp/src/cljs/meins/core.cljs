(ns meins.core
  (:require ["react-native" :refer [AppRegistry]]
            ["react-native-exception-handler" :refer [setJSExceptionHandler
                                                      setNativeExceptionHandler]]
            [meins.ui.shared :refer [alert]]
            [meins.ios.sync :as sync]
            [meins.store :as store]
            [meins.ios.photos :as photos]
            [meins.ui :as ui]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.scheduler :as sched]
            [meins.ios.healthkit :as hk]))

(enable-console-print!)
(defonce switchboard (sb/component :client/switchboard))
(def OBSERVER false)

(defn make-observable [components]
  (if OBSERVER
    (let [mapper #(assoc-in % [:opts :msgs-on-firehose] true)]
      (println "CORE: Attaching firehose")
      (set (mapv mapper components)))
    components))

(defn init []
  (let [components #{(hk/cmp-map :app/healthkit)
                     (store/cmp-map :app/store)
                     (photos/cmp-map :app/photos)
                     (sync/cmp-map :app/sync)
                     (sched/cmp-map :app/scheduler)
                     (ui/cmp-map :app/ui-cmp)}
        components (make-observable components)]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp components]

       [:cmd/route {:from :app/ui-cmp
                    :to   #{:app/store
                            :app/scheduler
                            :app/photos}}]

       [:cmd/observe-state {:from :app/store
                            :to   :app/ui-cmp}]

       [:cmd/route {:from :app/healthkit
                    :to   :app/store}]

       [:cmd/route {:from :app/ui-cmp
                    :to   :app/healthkit}]

       [:cmd/route {:from #{:app/store
                            :app/ui-cmp}
                    :to   :app/sync}]

       [:cmd/route {:from :app/sync
                    :to   #{:app/store
                            :app/scheduler}}]

       [:cmd/route {:from :app/store
                    :to   :app/scheduler}]

       [:cmd/route {:from :app/ui-cmp
                    :to   :app/photos}]

       [:cmd/route {:from :app/scheduler
                    :to   #{:app/store
                            :app/sync
                            :app/healthkit}}]

       #_(when OBSERVER
           [:cmd/attach-to-firehose :app/sync])

       [:cmd/send {:to  :app/scheduler
                   :msg [:cmd/schedule-new {:timeout 10000
                                            :message [:sync/fetch]
                                            :repeat  true
                                            :initial false}]}]])
    (.registerComponent AppRegistry "meins" #(identity ui/app-container))
    (setJSExceptionHandler (fn [error _is-fatal] (alert error)))))
