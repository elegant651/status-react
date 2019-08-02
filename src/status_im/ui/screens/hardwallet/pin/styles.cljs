(ns status-im.ui.screens.hardwallet.pin.styles
  (:require-macros [status-im.utils.styles :refer [defstyle]])
  (:require [status-im.ui.components.colors :as colors]))

(def container
  {:flex             1
   :background-color colors/white})

(defstyle pin-container
  {:flex            1
   :flex-direction  :column
   :justify-content :space-between
   :padding-bottom  10
   :android         {:margin-top 40}
   :ios             {:margin-top 30}})

(defstyle error-container
  {:android {:margin-top 25}
   :ios     {:margin-top 28}})

(def error-text
  {:color      colors/red
   :text-align :center})

(defn center-container [title]
  {:flex-direction :column
   :align-items    :center
   :margin-top     (if title 28 5)})

(def center-title-text
  {:typography :header})

(def create-pin-text
  {:padding-top 8
   :width       314
   :text-align  :center
   :color       colors/gray})

(def pin-indicator-container
  {:flex-direction  :row
   :justify-content :space-between
   :margin-top      30})

(def pin-indicator-group-container
  {:flex-direction  :row
   :justify-content :space-between})

(defn pin-indicator [pressed?]
  {:width             8
   :height            8
   :background-color  (if pressed?
                        colors/blue
                        colors/gray-light)
   :border-radius     50
   :margin-horizontal 5})

(def waiting-indicator-container
  {:margin-top 26})

(def numpad-container
  {:margin-top 20})

(def numpad-row-container
  {:flex-direction  :row
   :justify-content :center
   :align-items     :center
   :margin-vertical 12})

(def numpad-button
  {:width             64
   :margin-horizontal 16
   :height            64
   :align-items       :center
   :justify-content   :center
   :flex-direction    :row
   :border-radius     50
   :background-color  colors/gray-background})

(def numpad-delete-button
  (assoc numpad-button :background-color colors/white))

(def numpad-empty-button
  (assoc numpad-button :background-color colors/white
         :border-color colors/white))

(def numpad-button-text
  {:font-size 22
   :color     colors/blue})

(def numpad-empty-button-text
  {:color colors/white})
