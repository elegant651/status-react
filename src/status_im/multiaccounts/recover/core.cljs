(ns status-im.multiaccounts.recover.core
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]
            [status-im.multiaccounts.create.core :as multiaccounts.create]
            [status-im.multiaccounts.db :as db]
            [status-im.ethereum.mnemonic :as mnemonic]
            [status-im.i18n :as i18n]
            [status-im.native-module.core :as status]
            [status-im.node.core :as node]
            [status-im.ui.screens.navigation :as navigation]
            [status-im.utils.fx :as fx]
            [status-im.utils.identicon :as identicon]
            [status-im.utils.security :as security]
            [status-im.utils.types :as types]
            [status-im.constants :as constants]))

(defn check-password-errors [password]
  (cond (string/blank? password) :required-field
        (not (db/valid-length? password)) :recover-password-too-short))

(defn check-phrase-errors [recovery-phrase]
  (cond (string/blank? recovery-phrase) :required-field
        (not (mnemonic/valid-phrase? recovery-phrase)) :recovery-phrase-invalid))

(defn check-phrase-warnings [recovery-phrase]
  (when (not (mnemonic/status-generated-phrase? recovery-phrase))
    :recovery-phrase-unknown-words))

(defn recover-multiaccount! [masked-passphrase password]
  (status/recover-multiaccount
   (mnemonic/sanitize-passphrase (security/safe-unmask-data masked-passphrase))
   password
   (fn [result]
     ;; here we deserialize result, dissoc mnemonic and serialize the result again
     ;; because we want to have information about the result printed in logs, but
     ;; don't want secure data to be printed
     (let [data (-> (types/json->clj result)
                    (dissoc :mnemonic)
                    (types/clj->json))]
       (re-frame/dispatch [:multiaccounts.recover.callback/recover-multiaccount-success data password])))))

(fx/defn set-phrase
  [{:keys [db]} masked-recovery-phrase]
  (let [recovery-phrase (security/safe-unmask-data masked-recovery-phrase)]
    {:db (update db :multiaccounts/recover assoc
                 :passphrase (string/lower-case recovery-phrase)
                 :passphrase-valid? (not (check-phrase-errors recovery-phrase)))}))

(fx/defn validate-phrase
  [{:keys [db]}]
  (let [recovery-phrase (get-in db [:multiaccounts/recover :passphrase])]
    {:db (update db :multiaccounts/recover assoc
                 :passphrase-error (check-phrase-errors recovery-phrase)
                 :passphrase-warning (check-phrase-warnings recovery-phrase))}))

(fx/defn set-password
  [{:keys [db]} masked-password]
  (let [password (security/safe-unmask-data masked-password)]
    {:db (update db :multiaccounts/recover assoc
                 :password password
                 :password-valid? (not (check-password-errors password)))}))

(fx/defn validate-password
  [{:keys [db]}]
  (let [password (get-in db [:multiaccounts/recover :password])]
    {:db (assoc-in db [:multiaccounts/recover :password-error] (check-password-errors password))}))

(fx/defn validate-recover-result
  [{:keys [db] :as cofx} {:keys [error pubkey address walletAddress walletPubKey chatAddress chatPubKey]} password]
  (if (empty? error)
    (let [multiaccount-address (-> address
                                   (string/lower-case)
                                   (string/replace-first "0x" ""))
          keycard-multiaccount? (boolean (get-in db [:multiaccounts/multiaccounts multiaccount-address :keycard-instance-uid]))]
      (if keycard-multiaccount?
        ;; trying to recover multiaccount created with keycard
        {:db        (-> db
                        (update :multiaccounts/recover assoc
                                :processing? false
                                :passphrase-error :recover-keycard-multiaccount-not-supported)
                        (update :multiaccounts/recover dissoc
                                :passphrase-valid?))
         :node/stop nil}
        (let [multiaccount {:derived    {constants/path-whisper-keyword        {:publicKey chatPubKey
                                                                                :address chatAddress}
                                         constants/path-default-wallet-keyword {:publicKey walletPubKey
                                                                                :address walletAddress}}
                            :address    address
                            :mnemonic   ""}]
          (multiaccounts.create/on-multiaccount-created
           cofx multiaccount password {:seed-backed-up? true}))))
    {:db        (-> db
                    (update :multiaccounts/recover assoc
                            :processing? false
                            :password ""
                            :password-confirmation ""
                            :password-error :recover-password-invalid)
                    (update :multiaccounts/recover dissoc
                            :password-valid?))
     :node/stop nil}))

(fx/defn on-multiaccount-recovered
  [cofx result password]
  (let [data (types/json->clj result)]
    (validate-recover-result cofx data password)))

(fx/defn recover-multiaccount
  [{:keys [db random-guid-generator] :as cofx}]
  (fx/merge
   cofx
   {:db (-> db
            (assoc-in [:multiaccounts/recover :processing?] true)
            (assoc :node/on-ready :recover-multiaccount)
            (assoc :multiaccounts/new-installation-id (random-guid-generator)))}
   (node/initialize nil)))

(fx/defn recover-multiaccount-with-checks [{:keys [db] :as cofx}]
  (let [{:keys [passphrase processing?]} (:multiaccounts/recover db)]
    (when-not processing?
      (if (mnemonic/status-generated-phrase? passphrase)
        (recover-multiaccount cofx)
        {:ui/show-confirmation
         {:title               (i18n/label :recovery-typo-dialog-title)
          :content             (i18n/label :recovery-typo-dialog-description)
          :confirm-button-text (i18n/label :recovery-confirm-phrase)
          :on-accept           #(re-frame/dispatch [:multiaccounts.recover.ui/recover-multiaccount-confirmed])}}))))

(fx/defn navigate-to-recover-multiaccount-screen [{:keys [db] :as cofx}]
  (fx/merge cofx
            {:db (dissoc db :multiaccounts/recover)}
            (navigation/navigate-to-cofx :recover-multiaccount nil)))

(re-frame/reg-fx
 :multiaccounts.recover/recover-multiaccount
 (fn [[masked-passphrase password]]
   (recover-multiaccount! masked-passphrase password)))

(fx/defn re-encrypt-pressed
  {:events [:recover.success.ui/re-encrypt-pressed]}
  [{:keys [db] :as cofx}]
  (fx/merge cofx
            {:db (assoc-in db [:intro-wizard :selected-storage-type] :default)}
            (navigation/navigate-to-cofx :recover-multiaccount-select-storage nil)))

(fx/defn enter-phrase-pressed
  {:events [:recover.ui/enter-phrase-pressed]}
  [{:keys [db] :as cofx}]
  (fx/merge cofx
            {:dispatch [:bottom-sheet/hide-sheet]}
            (navigation/navigate-to-cofx :recover-multiaccount-enter-phrase nil)))

(fx/defn enter-phrase-next-button-pressed
  {:events [:recover.enter-passphrase.ui/input-submitted
            :recover.enter-passphrase.ui/next-pressed]}
  [{:keys [db] :as cofx}]
  (fx/merge cofx
            {:db (-> db
                     (assoc-in [:multiaccounts/recover :address] "0xF84E72a8b067993A758F12f8a25Bb839a27e8777")
                     (assoc-in [:multiaccounts/recover :pubkey] "0x044da4c5e58efe0cb20c42f3719682b8d17e78038a2a0e46483007da9747ada1984d7f053a2b2201ee7d476101be530021da642280e406fc57b4368ea88b43bdb4"))}
            (navigation/navigate-to-cofx :recover-multiaccount-success nil)))

(fx/defn cancel-pressed
  {:events [:recover.ui/cancel-pressed]}
  [cofx]
  (navigation/navigate-back cofx))

(fx/defn select-storage-next-pressed
  {:events [:recover.select-storage.ui/next-pressed]}
  [{:keys [db] :as cofx}]
  (let [storage-type (get-in db [:intro-wizard :selected-storage-type])]
    (if (= storage-type :advanced)
      {:dispatch [:recovery.ui/keycard-option-pressed]})
    (navigation/navigate-to-cofx cofx :recover-multiaccount-enter-password nil)))

(fx/defn enter-password-next-button-pressed
  {:events [:recover.enter-password.ui/input-submitted
            :recover.enter-password.ui/next-pressed]}
  [{:keys [db] :as cofx}]
  (fx/merge cofx
            (navigation/navigate-to-cofx :recover-multiaccount-confirm-password nil)))

(fx/defn confirm-password-next-button-pressed
  {:events       [:recover.confirm-password.ui/input-submitted
                  :recover.confirm-password.ui/next-pressed]
   :interceptors [(re-frame/inject-cofx :random-guid-generator)]}
  [{:keys [db] :as cofx}]
  (fx/merge cofx
            {:db (assoc db :intro-wizard nil)}
            (recover-multiaccount)
            (navigation/navigate-to-cofx :welcome nil)))

(fx/defn enter-phrase-input-changed
  {:events [:recover.enter-passphrase.ui/input-changed]}
  [{:keys [db]} input]
  {:db (assoc-in db [:multiaccounts/recover :passphrase] input)})

(fx/defn enter-password-input-changed
  {:events [:recover.enter-password.ui/input-changed]}
  [{:keys [db]} input]
  {:db (assoc-in db [:multiaccounts/recover :password] input)})

(fx/defn confirm-password-input-changed
  {:events [:recover.confirm-password.ui/input-changed]}
  [{:keys [db]} input]
  {:db (assoc-in db [:multiaccounts/recover :password-confirmation] input)})
