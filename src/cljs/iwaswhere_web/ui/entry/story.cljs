(ns iwaswhere-web.ui.entry.story
  (:require [iwaswhere-web.helpers :as h]
            [iwaswhere-web.ui.entry.capture :as c]
            [re-frame.core :refer [subscribe]]
            [reagent.ratom :refer-macros [reaction]]))

(defn editable-field
  [on-input-fn on-keydown-fn text]
  (fn [_ _ _]
    [:div.story-edit-field
     {:content-editable true
      :on-input         on-input-fn
      :on-key-down      on-keydown-fn}
     text]))

(defn keydown-fn
  [entry k put-fn]
  (fn [ev]
    (let [text (aget ev "target" "innerText")
          updated (assoc-in entry [k] text)
          key-code (.. ev -keyCode)
          meta-key (.. ev -metaKey)]
      (when (and meta-key (= key-code 83)) ; CMD-s pressed
        (put-fn [:entry/update updated])
        (.preventDefault ev)))))

(defn input-fn
  [entry k put-fn]
  (fn [ev]
    (let [text (aget ev "target" "innerText")
          updated (assoc-in entry [k] text)]
      (put-fn [:entry/update-local updated]))))

(defn story-name-field
  "Renders editable field for story name when the entry is of type :story.
   Updates local entry on input, and saves the entry when CMD-S is pressed."
  [entry edit-mode? put-fn]
  (when (= (:entry-type entry) :story)
    (let [on-input-fn (input-fn entry :story-name put-fn)
          on-keydown-fn (keydown-fn entry :story-name put-fn)]
      (if edit-mode?
        [:div.story
         [:label "Story:"]
         [editable-field on-input-fn on-keydown-fn (:story-name entry)]]
        [:h2 "Story: " (:story-name entry)]))))

(defn saga-name-field
  "Renders editable field for saga name when the entry is of type :saga.
   Updates local entry on input, and saves the entry when CMD-S is pressed."
  [entry edit-mode? put-fn]
  (when (= (:entry-type entry) :saga)
    (let [on-input-fn (input-fn entry :saga-name put-fn)
          on-keydown-fn (keydown-fn entry :saga-name put-fn)]
      (if edit-mode?
        [:div.story
         [:label "Saga:"]
         [editable-field on-input-fn on-keydown-fn (:saga-name entry)]]
        [:h2 "Saga: " (:saga-name entry)]))))

(defn story-select
  "In edit mode, allow editing of story, otherwise show story name."
  [entry put-fn edit-mode?]
  (let [options (subscribe [:options])
        stories (reaction (:stories @options))
        sorted-stories (reaction (:sorted-stories @options))
        ts (:timestamp entry)
        new-entries (subscribe [:new-entries])
        select-handler
        (fn [ev]
          (let [selected (js/parseInt (-> ev .-nativeEvent .-target .-value))
                updated (-> (get-in @new-entries [ts])
                            (assoc-in [:linked-story] selected))]
            (put-fn [:entry/update-local updated])))]
    (fn story-select-render [entry put-fn edit-mode?]
      (let [linked-story (:linked-story entry)]
        (if edit-mode?
          (when-not (or (contains? #{:saga :story} (:entry-type entry))
                        (:comment-for entry))
            [:div.story
             [:label "Story:"]
             [:select {:value     (or linked-story "")
                       :on-change select-handler}
              [:option {:value ""} "no story selected"]
              (for [[id story] @sorted-stories]
                (let [story-name (:story-name story)]
                  ^{:key (str ts story-name)}
                  [:option {:value id} story-name]))]])
          (when linked-story
            [:div.story (:story-name (get @stories linked-story))]))))))

(defn saga-select
  "In edit mode, allow editing of story, otherwise show story name."
  [entry put-fn edit-mode?]
  (let [options (subscribe [:options])
        sagas (reaction (:sagas @options))
        sorted-sagas (reaction (:sorted-sagas @options))
        ts (:timestamp entry)
        new-entries (subscribe [:new-entries])
        select-handler
        (fn [ev]
          (let [selected (js/parseInt (-> ev .-nativeEvent .-target .-value))
                updated (-> (get-in @new-entries [ts])
                            (assoc-in [:linked-saga] selected))]
            (put-fn [:entry/update-local updated])))]
    (fn story-select-render [entry put-fn edit-mode?]
      (let [linked-saga (:linked-saga entry)
            entry-type (:entry-type entry)]
        (when (= entry-type :story)
          (if edit-mode?
            (when-not (:comment-for entry)
              [:div.story
               [:label "Saga:"]
               [:select {:value     (or linked-saga "")
                         :on-change select-handler}
                [:option {:value ""} "no saga selected"]
                (for [[id saga] @sorted-sagas]
                  (let [saga-name (:saga-name saga)]
                    ^{:key (str ts saga-name)}
                    [:option {:value id} saga-name]))]])
            (when linked-saga
              [:div.story "Saga: " (:saga-name (get @sagas linked-saga))])))))))
