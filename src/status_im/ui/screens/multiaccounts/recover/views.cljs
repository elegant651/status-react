(ns status-im.ui.screens.multiaccounts.recover.views
  (:require-macros [status-im.utils.views :refer [defview letsubs]
                    :as views])
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [status-im.ui.components.text-input.view :as text-input]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.status-bar.view :as status-bar]
            [status-im.ui.components.toolbar.view :as toolbar]
            [status-im.i18n :as i18n]
            [status-im.ui.screens.multiaccounts.recover.styles :as styles]
            [status-im.ui.components.styles :as components.styles]
            [status-im.utils.config :as config]
            [status-im.utils.core :as utils.core]
            [status-im.react-native.js-dependencies :as js-dependencies]
            [status-im.ui.components.common.common :as components.common]
            [status-im.utils.security :as security]
            [status-im.utils.platform :as platform]
            [clojure.string :as string]
            [status-im.ui.components.action-button.styles :as action-button.styles]
            [status-im.ui.components.action-button.action-button :as action-button]
            [status-im.ui.components.colors :as colors]
            [status-im.utils.gfycat.core :as gfy]
            [status-im.utils.identicon :as identicon]
            [status-im.ui.components.radio :as radio]
            [status-im.ui.components.icons.vector-icons :as vector-icons]))

(defview passphrase-input [passphrase error warning]
  (letsubs [input-ref (reagent/atom nil)]
    [text-input/text-input-with-label
     {:style               styles/recovery-phrase-input
      :height              92
      :ref                 (partial reset! input-ref)
      :label               (i18n/label :t/recovery-phrase)
      :accessibility-label :enter-12-words
      :placeholder         (i18n/label :t/enter-12-words)
      :multiline           true
      :default-value       passphrase
      :auto-correct        false
      :on-change-text      #(re-frame/dispatch [:multiaccounts.recover.ui/passphrase-input-changed (security/mask-data %)])
      :on-blur             #(re-frame/dispatch [:multiaccounts.recover.ui/passphrase-input-blured])
      :error               (cond error (i18n/label error)
                                 warning (i18n/label warning))}]))

(defview password-input [password error on-submit-editing]
  (views/letsubs [inp-ref (atom nil)]
    {:component-will-update
     (fn [_ [_ new-password]]
       (when (and (string? new-password)
                  (string/blank? new-password)
                  @inp-ref)
         (.clear @inp-ref)))}
    [react/view {:style                       styles/password-input
                 :important-for-accessibility :no-hide-descendants}
     [text-input/text-input-with-label
      {:label               (i18n/label :t/password)
       :accessibility-label :enter-password
       :placeholder         (i18n/label :t/enter-password)
       :default-value       password
       :auto-focus          false
       :on-change-text      #(re-frame/dispatch [:multiaccounts.recover.ui/password-input-changed (security/mask-data %)])
       :on-blur             #(re-frame/dispatch [:multiaccounts.recover.ui/password-input-blured])
       :secure-text-entry   true
       :error               (when error (i18n/label error))
       :on-submit-editing   on-submit-editing
       :ref                 #(reset! inp-ref %)}]]))

(defview recover []
  (letsubs [recovered-multiaccount [:get-recover-multiaccount]
            node-status? [:node-status]]
    (let [{:keys [passphrase password passphrase-valid? password-valid?
                  password-error passphrase-error passphrase-warning processing?]} recovered-multiaccount
          node-stopped? (or (nil? node-status?)
                            (= :stopped node-status?))
          valid-form? (and password-valid? passphrase-valid?)
          disabled?   (or (not recovered-multiaccount)
                          processing?
                          (not valid-form?)
                          (not node-stopped?))
          sign-in     #(re-frame/dispatch [:multiaccounts.recover.ui/sign-in-button-pressed])]
      [react/keyboard-avoiding-view {:style styles/screen-container}
       [status-bar/status-bar]
       [toolbar/toolbar nil toolbar/default-nav-back
        [toolbar/content-title (i18n/label :t/sign-in-to-another)]]
       [react/view styles/inputs-container
        [passphrase-input (or passphrase "") passphrase-error passphrase-warning]
        [password-input (or password "") password-error (when-not disabled? sign-in)]
        (when platform/desktop?
          [react/i18n-text {:style styles/recover-release-warning
                            :key   :recover-multiaccount-warning}])]
       [react/view components.styles/flex]
       (if processing?
         [react/view styles/processing-view
          [react/activity-indicator {:animating true}]
          [react/i18n-text {:style styles/sign-you-in
                            :key   :sign-you-in}]]
         [react/view {:style styles/bottom-button-container}
          [react/view {:style components.styles/flex}]
          [components.common/bottom-button
           {:forward?  true
            :label     (i18n/label :t/sign-in)
            :disabled? disabled?
            :on-press  sign-in}]])])))

(defn bottom-sheet-view []
  [react/view {:flex 1 :flex-direction :row}
   [react/view action-button.styles/actions-list
    [action-button/action-button
     {:label               (i18n/label :t/enter-seed-phrase)
      :accessibility-label :enter-seed-phrase-button
      :icon                :main-icons/text
      :icon-opts           {:color colors/blue}
      :on-press            #(re-frame/dispatch [:recover.ui/enter-phrase-pressed])}]
    [action-button/action-button
     {:label               (i18n/label :t/recover-with-keycard)
      :accessibility-label :recover-with-keycard-button
      :icon                :main-icons/keycard-logo
      :icon-opts           {:color colors/blue}
      :on-press            #(re-frame/dispatch [:recover.ui/recover-with-keycard-pressed])}]]])

(def bottom-sheet
  {:content        bottom-sheet-view
   :content-height 130})

(defview enter-phrase []
  (letsubs [mnemonic [:multiaccounts.recover/passphrase]
            processing? [:multiaccounts.recover/processing?]]
    [react/view {:flex             1
                 :justify-content  :space-between
                 :background-color colors/white}
     [toolbar/toolbar
      {:transparent? true
       :style        {:margin-top 32}}
      [toolbar/nav-text
       {:handler #(re-frame/dispatch [:recover.ui/cancel-pressed])
        :style   {:padding-left 21}}
       (i18n/label :t/cancel)]
      [react/text {:style {:color colors/gray}}
       (i18n/label :t/step-i-of-n {:step   "1"
                                   :number "2"})]]
     [react/view {:flex            1
                  :flex-direction  :column
                  :justify-content :space-between
                  :align-items     :center}
      [react/view {:flex-direction :column
                   :align-items    :center}
       [react/view {:margin-top 16}
        [react/text {:style {:typography :header
                             :text-align :center}}
         (i18n/label :t/multiaccounts-recover-enter-phrase-title)]]
       [react/view {:margin-top  16
                    :width       "85%"
                    :align-items :center}
        [react/text {:style {:color      colors/gray
                             :text-align :center}}
         (i18n/label :t/multiaccounts-recover-enter-phrase-text)]]
       [react/view {:margin-top 16}
        [text-input/text-input-with-label
         {:on-change-text    #(re-frame/dispatch [:recover.enter-passphrase.ui/input-changed %])
          :auto-focus        true
          :on-submit-editing #(re-frame/dispatch [:recover.enter-passphrase.ui/input-submitted])
          :placeholder       nil
          :height            125
          :multiline         true
          :auto-correct      false
          :container         {:background-color :white}
          :style             {:background-color :white
                              :text-align       :center
                              :typography       :header}}]]]
      (when processing?
        [react/view
         [react/activity-indicator {:size      :large
                                    :animating true}]
         [react/text {:style {:color      colors/gray
                              :margin-top 8}}
          (i18n/label :t/processing)]])
      [react/view {:flex-direction  :row
                   :justify-content :space-between
                   :align-items     :center
                   :width           "100%"
                   :height          86}
       (when-not processing?
         [react/view])
       (when-not processing?
         [react/view {:margin-right 20}
          [components.common/bottom-button
           {:on-press  #(re-frame/dispatch [:recover.enter-passphrase.ui/next-pressed])
            :label     (i18n/label :t/next)
            :disabled? (empty? mnemonic)
            :forward?  true}]])]]]))

(defview success []
  (letsubs [address [:multiaccounts.recover/address]
            pubkey [:multiaccounts.recover/pubkey]]
    [react/view {:flex             1
                 :justify-content  :space-between
                 :background-color colors/white}
     [toolbar/toolbar
      {:transparent? true
       :style        {:margin-top 32}}
      nil
      nil]
     [react/view {:flex            1
                  :flex-direction  :column
                  :justify-content :space-between
                  :align-items     :center}
      [react/view {:flex-direction :column
                   :align-items    :center}
       [react/view {:margin-top 16}
        [react/text {:style {:typography :header
                             :text-align :center}}
         (i18n/label :t/keycard-recovery-success-header)]]
       [react/view {:margin-top  16
                    :width       "85%"
                    :align-items :center}
        [react/text {:style {:color      colors/gray
                             :text-align :center}}
         (i18n/label :t/recovery-success-text)]]]
      [react/view {:flex-direction  :column
                   :flex            1
                   :justify-content :center
                   :align-items     :center}
       [react/view {:margin-horizontal 16
                    :flex-direction    :column}
        [react/view {:justify-content :center
                     :align-items     :center
                     :margin-bottom   11}
         [react/image {:source {:uri (identicon/identicon pubkey)}
                       :style  {:width         61
                                :height        61
                                :border-radius 30
                                :border-width  1
                                :border-color  (colors/alpha colors/black 0.1)}}]]
        [react/text {:style           {:text-align  :center
                                       :color       colors/black
                                       :font-weight "500"}
                     :number-of-lines 1
                     :ellipsize-mode  :middle}
         (gfy/generate-gfy pubkey)]
        [react/text {:style           {:text-align  :center
                                       :margin-top  4
                                       :color       colors/gray
                                       :font-family "monospace"}
                     :number-of-lines 1
                     :ellipsize-mode  :middle}
         (utils.core/truncate-str address 14 true)]]]
      [react/view {:margin-bottom 50}
       [react/touchable-highlight
        {:on-press #(re-frame/dispatch [:recover.success.ui/re-encrypt-pressed])}
        [react/view {:background-color colors/gray-background
                     :align-items      :center
                     :justify-content  :center
                     :flex-direction   :row
                     :width            193
                     :height           44
                     :border-radius    10}
         [react/text {:style {:color colors/blue}}
          (i18n/label :t/re-encrypt-key)]]]]]]))

(defview select-storage []
  (letsubs [{:keys [selected-storage-type]} [:intro-wizard]
            {view-height :height} [:dimensions/window]]
    [react/view {:flex             1
                 :justify-content  :space-between
                 :background-color colors/white}
     [toolbar/toolbar
      {:transparent? true
       :style        {:margin-top 32}}
      [toolbar/nav-text
       {:handler #(re-frame/dispatch [:recover.ui/cancel-pressed])
        :style   {:padding-left 21}}
       (i18n/label :t/cancel)]
      nil]
     [react/view {:flex            1
                  :justify-content :space-between}
      [react/view {:flex-direction :column
                   :align-items    :center}
       [react/view {:margin-top 16}
        [react/text {:style {:typography :header
                             :text-align :center}}
         (i18n/label :t/intro-wizard-title3)]]
       [react/view {:margin-top  16
                    :width       "85%"
                    :align-items :center}
        [react/text {:style {:color      colors/gray
                             :text-align :center}}
         (i18n/label :t/intro-wizard-text3)]]]
      [status-im.ui.screens.intro.views/select-key-storage {:selected-storage-type selected-storage-type} view-height]
      [react/view {:flex-direction  :row
                   :justify-content :space-between
                   :align-items     :center
                   :width           "100%"
                   :height          86}
       [react/view components.styles/flex]
       [react/view {:margin-right 20}
        [components.common/bottom-button
         {:on-press #(re-frame/dispatch [:recover.select-storage.ui/next-pressed])
          :forward? true}]]]]]))

(defview enter-password []
  (letsubs [password [:multiaccounts.recover/password]]
    [react/view {:flex             1
                 :justify-content  :space-between
                 :background-color colors/white}
     [toolbar/toolbar
      {:transparent? true
       :style        {:margin-top 32}}
      [toolbar/nav-text
       {:handler #(re-frame/dispatch [:recover.ui/cancel-pressed])
        :style   {:padding-left 21}}
       (i18n/label :t/cancel)]
      [react/text {:style {:color colors/gray}}
       (i18n/label :t/step-i-of-n {:step   "1"
                                   :number "2"})]]
     [react/view {:flex            1
                  :flex-direction  :column
                  :justify-content :space-between
                  :align-items     :center}
      [react/view {:flex-direction :column
                   :align-items    :center}
       [react/view {:margin-top 16}
        [react/text {:style {:typography :header
                             :text-align :center}}
         (i18n/label :t/intro-wizard-title-alt4)]]
       [react/view {:margin-top  16
                    :width       "85%"
                    :align-items :center}
        [react/text {:style {:color      colors/gray
                             :text-align :center}}
         (i18n/label :t/password-description)]]
       [react/view {:margin-top 16}
        [text-input/text-input-with-label
         {:on-change-text    #(re-frame/dispatch [:recover.enter-password.ui/input-changed %])
          :auto-focus        true
          :on-submit-editing #(re-frame/dispatch [:recover.enter-password.ui/input-submitted])
          :placeholder       nil
          :height            125
          :multiline         false
          :auto-correct      false
          :container         {:background-color :white}
          :style             {:background-color :white
                              :text-align       :center
                              :typography       :header}}]]]
      [react/view {:flex-direction  :row
                   :justify-content :space-between
                   :align-items     :center
                   :width           "100%"
                   :height          86}
       [react/view]
       [react/view {:margin-right 20}
        [components.common/bottom-button
         {:on-press  #(re-frame/dispatch [:recover.enter-password.ui/next-pressed])
          :label     (i18n/label :t/next)
          :disabled? (empty? password)
          :forward?  true}]]]]]))

(defview confirm-password []
  (letsubs [password [:multiaccounts.recover/password-confirmation]]
    [react/view {:flex             1
                 :justify-content  :space-between
                 :background-color colors/white}
     [toolbar/toolbar
      {:transparent? true
       :style        {:margin-top 32}}
      [toolbar/nav-text
       {:handler #(re-frame/dispatch [:recover.ui/cancel-pressed])
        :style   {:padding-left 21}}
       (i18n/label :t/cancel)]
      [react/text {:style {:color colors/gray}}
       (i18n/label :t/step-i-of-n {:step   "1"
                                   :number "2"})]]
     [react/view {:flex            1
                  :flex-direction  :column
                  :justify-content :space-between
                  :align-items     :center}
      [react/view {:flex-direction :column
                   :align-items    :center}
       [react/view {:margin-top 16}
        [react/text {:style {:typography :header
                             :text-align :center}}
         (i18n/label :t/intro-wizard-title-alt5)]]
       [react/view {:margin-top  16
                    :width       "85%"
                    :align-items :center}
        [react/text {:style {:color      colors/gray
                             :text-align :center}}
         (i18n/label :t/password-description)]]
       [react/view {:margin-top 16}
        [text-input/text-input-with-label
         {:on-change-text    #(re-frame/dispatch [:recover.confirm-password.ui/input-changed %])
          :auto-focus        true
          :on-submit-editing #(re-frame/dispatch [:recover.confirm-password.ui/input-submitted])
          :placeholder       nil
          :height            125
          :multiline         false
          :auto-correct      false
          :container         {:background-color :white}
          :style             {:background-color :white
                              :text-align       :center
                              :typography       :header}}]]]
      [react/view {:flex-direction  :row
                   :justify-content :space-between
                   :align-items     :center
                   :width           "100%"
                   :height          86}
       [react/view]
       [react/view {:margin-right 20}
        [components.common/bottom-button
         {:on-press  #(re-frame/dispatch [:recover.confirm-password.ui/next-pressed])
          :label     (i18n/label :t/next)
          :disabled? (empty? password)
          :forward?  true}]]]]]))
