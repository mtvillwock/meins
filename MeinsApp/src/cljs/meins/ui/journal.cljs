(ns meins.ui.journal
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe]]
            [meins.helpers :as h]
            [glittershark.core-async-storage :as as]
            [meins.ui.colors :as c]
            [meins.ui.db :as uidb :refer [emit]]
            [meins.ui.editor :as ed]
            [cljs.reader :as rdr]
            ["react-navigation-transitions" :refer [fadeIn]]
            [meins.ui.shared :refer [view text text-input scroll search-bar flat-list
                                     #_map-view #_mapbox-style-url #_point-annotation virtualized-list
                                     fa-icon image #_swipeout keyboard-avoiding-view
                                     touchable-opacity settings-list settings-list-item platform-os
                                     rn-audio-recorder-player alert status-bar]]
            ["react-navigation" :refer [createStackNavigator createAppContainer]]
            [clojure.pprint :as pp]
            [meins.utils.parse :as p]))

(defn get-entry [ts]
  (when (number? ts)
    (some-> @uidb/realm-db
            (.objects "Entry")
            (.filtered (str "timestamp = " ts))
            (aget 0 "edn")
            rdr/read-string)))

(defn list-item [_ts _navigate]
  (let [theme (subscribe [:active-theme])
        global-vclock (subscribe [:global-vclock])
        cfg (subscribe [:cfg])]
    (fn list-item-render [ts navigate]
      @global-vclock
      (let [text-bg (get-in c/colors [:text-bg @theme])
            text-color (get-in c/colors [:text @theme])
            show-pvt (:show-pvt @cfg)
            entry (get-entry ts)
            to-detail #(do (emit [:entry/detail {:timestamp ts}])
                           (navigate "Detail"))
            {:keys [md]} entry
            md (if (> (count md) 100)
                 (str (subs md 0 100) "...")
                 md)
            delete #(emit [:entry/persist (assoc-in entry [:deleted] true)])]
        (when (or (not (or (:pvt entry)
                           (:pvt (:story entry))
                           (-> entry :story :saga :pvt)
                           (contains? (:tags entry) "#pvt")
                           (contains? (:perm_tags entry) "#pvt")))
                  show-pvt)
          [view {:style {:flex             1
                         :margin-bottom    4
                         :flex-direction   :row
                         :background-color "black"
                         :width            "100%"}}
           [touchable-opacity {:on-press to-detail
                               :style    {:display         "flex"
                                          :flex-direction  "column"
                                          :width           "100%"
                                          :justify-content "space-between"}}
            (when-let [media (:media entry)]
              [image {:style  {:width  "100%"
                               :height 300}
                      :source {:uri (-> media :image :uri)}}])
            (when-let [spotify (:spotify entry)]
              [image {:style      {:background-color "black"
                                   :height           150
                                   :width            "100%"}
                      :resizeMode "contain"
                      :source     {:uri (:image spotify)}}])
            [view {:style {:flex             1
                           :flex-direction   :column
                           :background-color text-bg
                           :padding-top      4
                           :padding-left     8
                           :padding-right    6
                           :padding-bottom   4
                           :width            "100%"}}
             [view {:style {:padding-top    2
                            :padding-left   4
                            :padding-right  4
                            :padding-bottom 2}}
              [text {:style {:color       text-color
                             :text-align  "left"
                             :font-size   9
                             :font-weight "100"}}
               (h/format-time ts)]]
             (if-let [spotify (:spotify entry)]
               [view {:style {:padding-top    1
                              :padding-left   4
                              :padding-right  4
                              :padding-bottom 4}}
                [text {:style {:background-color text-bg
                               :color            text-color
                               :text-align       "left"
                               :font-weight      "bold"
                               :font-size        12}}
                 (:name spotify)]
                [text {:style {:background-color text-bg
                               :color            text-color
                               :text-align       "left"
                               :font-size        12
                               :padding-top      1}}
                 (->> (:artists spotify)
                      (map :name)
                      (interpose ", ")
                      (apply str))]]
               [view {:style {:padding-top    1
                              :padding-left   4
                              :padding-right  4
                              :padding-bottom 4}}
                [text {:style {:color       text-color
                               :text-align  "left"
                               :font-weight "normal"}}
                 md]])]]])))))

(defn render-item [navigate]
  (fn [item]
    (let [item (js->clj item :keywordize-keys true)
          entry (:item item)
          ts (:timestamp entry)]
      (r/as-element [list-item ts navigate]))))

(defn search-field [local]
  (let [theme (subscribe [:active-theme])
        on-change-text #(swap! local assoc-in [:jrn-search] %)
        on-clear-text #(swap! local assoc-in [:jrn-search] "")]
    (fn [_local]
      (let [light-theme (= :light @theme)
            search-field-bg (get-in c/colors [:search-field-bg @theme])
            header-tab-bg (get-in c/colors [:header-tab @theme])
            pt (if (= platform-os "ios") 40 10)]
        [view {:style {:background-color header-tab-bg
                       :padding-top      pt
                       :padding-bottom   6}}
         [search-bar {:placeholder         "search..."
                      :lightTheme          light-theme
                      :on-change-text      on-change-text
                      :on-clear-text       on-clear-text
                      :value               (:jrn-search @local)
                      :keyboard-type       "twitter"
                      :keyboardAppearance  (if light-theme "light" "dark")
                      :inputContainerStyle {:backgroundColor search-field-bg}
                      :containerStyle      {:backgroundColor   "transparent"
                                            :borderTopWidth    0
                                            :borderBottomWidth 0}}]]))))

(defn journal [_]
  (let [theme (subscribe [:active-theme])
        global-vclock (subscribe [:global-vclock])
        local (r/atom {:jrn-search ""})
        realm-db @uidb/realm-db]
    (fn [{:keys [navigation] :as props}]
      (let [{:keys [navigate] :as n} (js->clj navigation :keywordize-keys true)
            res (some-> realm-db
                        (.objects "Entry")
                        (.filtered (str "md CONTAINS[c] \"" (:jrn-search @local) "\""))
                        (.sorted "timestamp" true)
                        (.slice 0 1000))
            as-array (clj->js (map (fn [ts] {:timestamp (.-timestamp ts)}) res))
            bg (get-in c/colors [:list-bg @theme])]
        @global-vclock
        [view {:style {:flex             1
                       :height           "100%"
                       :background-color bg}}
         [status-bar {:barStyle "light-content"}]
         [search-field local]
         [flat-list {:style        {:flex           1
                                    :padding-bottom 50
                                    :width          "100%"}
                     :keyExtractor (fn [item] (aget item "timestamp"))
                     :data         as-array
                     :render-item  (render-item navigate)}]]))))

(defn entry-detail [_]
  (let [entry-detail (subscribe [:entry-detail])
        theme (subscribe [:active-theme])
        cfg (subscribe [:cfg])
        player-state (r/atom {:pos    0
                              :status :paused})
        recorder-player (rn-audio-recorder-player.)
        entry-local (r/atom {:entry {}})]
    (fn [{:keys [navigation] :as _props}]
      (let [{:keys [navigate _goBack] :as _nav} (js->clj navigation :keywordize-keys true)
            entry (get-entry (:timestamp @entry-detail))
            bg (get-in c/colors [:list-bg @theme])
            text-bg (get-in c/colors [:text-bg @theme])
            text-color (get-in c/colors [:text @theme])
            latitude (:latitude entry)
            longitude (:longitude entry)
            save-fn (fn []
                      (let [updated (p/parse-entry (:md @entry-local))]
                        (emit [:entry/persist (merge entry updated)])
                        (reset! entry-local {})
                        (navigate "Journal")))
            cancel-fn (fn [] (navigate "Journal"))
            pt (if (= platform-os "ios") 40 10)]
        ;(reset! nav navigation)
        [view {:style {:display          "flex"
                       :flex-direction   "column"
                       :height           "100%"
                       :background-color bg
                       :padding-top      pt}}
         [status-bar {:barStyle "light-content"}]
         [ed/header save-fn cancel-fn "Edit"]
         [keyboard-avoiding-view {:behavior "padding"
                                  :style    {:display         "flex"
                                             :flex-direction  "column"
                                             :justify-content "space-between"
                                             :width           "100%"
                                             :flex            1
                                             :align-items     "center"}}
          [scroll {:style {:flex-direction   "column"
                           :background-color bg
                           :min-height       250
                           :width            "100%"
                           :padding-bottom   10}}
           (when-let [media (:media entry)]
             [image {:style      {:flex             3
                                  :background-color "black"
                                  :min-height       300
                                  :max-height       600
                                  :width            "100%"}
                     :resizeMode "contain"
                     :source     {:uri (-> media :image :uri)}}])
           [text {:style {:background-color text-bg
                          :color            text-color
                          :text-align       "center"
                          :font-size        12
                          :padding          4}}
            (h/format-time (:timestamp entry))]
           (if-let [spotify (:spotify entry)]
             [view {:style {:display          "flex"
                            :flex-direction   "column"
                            :background-color "white"}}
              [image {:style      {:flex             3
                                   :background-color "black"
                                   :min-height       300
                                   :max-height       600
                                   :width            "100%"}
                      :resizeMode "contain"
                      :source     {:uri (:image spotify)}}]
              [text {:style {:background-color text-bg
                             :color            text-color
                             :text-align       "left"
                             :font-weight      "bold"
                             :font-size        12
                             :padding-left     12
                             :padding-top      4}}
               (:name spotify)]
              [text {:style {:background-color text-bg
                             :color            text-color
                             :text-align       "left"
                             :font-size        12
                             :padding-left     12
                             :padding-top      1
                             :padding-bottom   4}}
               (->> (:artists spotify)
                    (map :name)
                    (interpose ", ")
                    (apply str))]]
             [text-input {:style              {:flex             2
                                               :font-weight      "100"
                                               :padding          16
                                               :font-size        24
                                               :max-height       400
                                               :min-height       100
                                               :background-color text-bg
                                               :margin-bottom    5
                                               :color            text-color
                                               :width            "100%"}
                          :multiline          true
                          :default-value      (:md entry "")
                          :keyboard-type      "twitter"
                          :keyboardAppearance (if (= @theme :dark) "dark" "light")
                          :on-change-text     (fn [text]
                                                (swap! entry-local assoc-in [:md] text))}])
           #_(when (and latitude longitude (= platform-os "ios"))
               [map-view {:centerCoordinate [longitude latitude]
                          :scrollEnabled    false
                          :rotateEnabled    false
                          :styleURL         (get mapbox-style-url :Street)
                          :style            {:width         "100%"
                                             :height        250
                                             :margin-bottom 30}
                          :zoomLevel        15}
                [point-annotation {:coordinate [longitude latitude]
                                   :id         (str (:timestamp entry))}
                 [view {:style {:width           24
                                :height          24
                                :alignItems      "center"
                                :justifyContent  "center"
                                :backgroundColor "white"
                                :borderRadius    12}}
                  [view {:style {:width           24
                                 :height          24
                                 :backgroundColor "orange"
                                 :borderRadius    12
                                 :transform       [{:scale 0.7}]}}]]]])
           (when-let [audio-file (:audio_file entry)]
             (let [status (:status @player-state)
                   prefix (when (= "android" platform-os)
                            "/data/data/com.matthiasn.meins/")
                   pos (h/mm-ss (.floor js/Math (:pos @player-state)))
                   play (fn [_]
                          (.startPlayer recorder-player (str prefix audio-file))
                          (.addPlayBackListener
                            recorder-player
                            #(swap! player-state assoc-in [:pos] (.-current_position %)))
                          (swap! player-state assoc-in [:status] :play))
                   stop (fn [_]
                          (.stopPlayer recorder-player)
                          (.removePlayBackListener recorder-player)
                          (swap! player-state assoc-in [:status] :paused))]
               [touchable-opacity {:on-press (if (= :play status) stop play)
                                   :style    {:margin         10
                                              :display        "flex"
                                              :flex-direction "row"}}
                [fa-icon {:name  "microphone"
                          :size  30
                          :style {:color       (if (= :play status) "#66F" "#999")
                                  :margin-left 25}}]
                [text {:style {:color       "#0078e7"
                               :font-size   30
                               :margin-left 25
                               :font-family "Courier"}}
                 (if (= :play status) "Stop" "Play")]
                [text {:style {:font-size    30
                               :color        "#888"
                               :font-weight  "100"
                               :margin-left  50
                               :margin-right 25
                               :font-family  "Courier"}}
                 pos]]))]
          (when (:entry-pprint @cfg)
            [text {:style {:margin-top 4
                           :color      "white"
                           :text-align "left"
                           :font-size  8}}
             (with-out-str (pp/pprint entry))])]]))))

(def journal-stack
  (createStackNavigator
    (clj->js {:Journal {:screen (r/reactify-component journal)}
              :Detail  {:screen (r/reactify-component entry-detail)}})
    (clj->js {:headerMode               "none"
              :defaultNavigationOptions {:headerStyle {:backgroundColor   "#445"
                                                       :borderBottomWidth 0}}
              :transitionConfig         (fn [] (fadeIn 200))})))
