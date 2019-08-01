(ns status-im.ui.screens.multiaccounts.recover.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :multiaccounts.recover/passphrase
 (fn [db]
   (get-in db [:multiaccounts/recover :passphrase])))

(re-frame/reg-sub
 :multiaccounts.recover/password
 (fn [db]
   (get-in db [:multiaccounts/recover :password])))

(re-frame/reg-sub
 :multiaccounts.recover/password-confirmation
 (fn [db]
   (get-in db [:multiaccounts/recover :password-confirmation])))

(re-frame/reg-sub
 :multiaccounts.recover/address
 (fn [db]
   (get-in db [:multiaccounts/recover :address])))

(re-frame/reg-sub
 :multiaccounts.recover/pubkey
 (fn [db]
   (get-in db [:multiaccounts/recover :pubkey])))

(re-frame/reg-sub
 :multiaccounts.recover/processing?
 (fn [db]
   (get-in db [:multiaccounts/recover :processing?])))
