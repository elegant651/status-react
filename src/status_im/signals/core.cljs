(ns status-im.signals.core
  (:require [status-im.multiaccounts.model :as multiaccounts.model]
            [status-im.multiaccounts.login.core :as multiaccounts.login]
            [status-im.chat.models.loading :as chat.loading]
            [status-im.contact-recovery.core :as contact-recovery]
            [status-im.ethereum.subscriptions :as ethereum.subscriptions]
            [status-im.hardwallet.core :as hardwallet]
            [status-im.mailserver.core :as mailserver]
            [status-im.node.core :as node]
            [status-im.pairing.core :as pairing]
            [status-im.transport.message.core :as transport.message]
            [status-im.transport.filters.core :as transport.filters]
            [status-im.utils.fx :as fx]
            [status-im.utils.security :as security]
            [status-im.utils.types :as types]
            [taoensso.timbre :as log]
            [re-frame.core :as re-frame]))

(fx/defn status-node-started
  [{db :db :as cofx} event]
  (let [[address nodes] (:realm/started? db)]
    (cond-> {:db (-> db
                     (assoc :node/status :started)
                     (dissoc :node/restart? :node/address))}
      (:realm/started? db)
      (assoc :dispatch [:init.callback/multiaccount-change-success address nodes]))))

(fx/defn status-node-stopped
  [{db :db}]
  {:db (assoc db :node/status :stopped)})

(fx/defn summary
  [{:keys [db] :as cofx} peers-summary]
  (let [previous-summary (:peers-summary db)
        peers-count      (count peers-summary)]
    (fx/merge cofx
              {:db (assoc db
                          :peers-summary peers-summary
                          :peers-count peers-count)}
              (transport.message/resend-contact-messages previous-summary)
              (mailserver/peers-summary-change previous-summary))))

(fx/defn process
  [cofx event-str]
  (let [{:keys [type event]} (types/json->clj event-str)]
    (case type
      "node.login"         (status-node-started cofx event)
      "node.stopped"       (status-node-stopped cofx)
      "envelope.sent"      (transport.message/update-envelope-status cofx (:hash event) :sent)
      "envelope.expired"   (transport.message/update-envelope-status cofx (:hash event) :not-sent)
      "bundles.added"      (pairing/handle-bundles-added cofx event)
      "mailserver.request.completed" (mailserver/handle-request-completed cofx event)
      "mailserver.request.expired"   (when (multiaccounts.model/logged-in? cofx)
                                       (mailserver/resend-request cofx {:request-id (:hash event)}))
      "messages.decrypt.failed" (contact-recovery/handle-contact-recovery-fx cofx (:sender event))
      "discovery.summary"  (summary cofx event)
      "subscriptions.data" (ethereum.subscriptions/handle-signal cofx event)
      "subscriptions.error" (ethereum.subscriptions/handle-error cofx event)
      "status.chats.did-change" (chat.loading/load-chats-from-rpc cofx)
      "whisper.filter.added" (transport.filters/handle-negotiated-filter cofx event)
      "messages.new" (transport.message/receive-messages cofx event)
      "wallet" (ethereum.subscriptions/new-wallet-event cofx event)
      (log/debug "Event " type " not handled" event))))
