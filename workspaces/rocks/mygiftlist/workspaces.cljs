(ns rocks.mygiftlist.workspaces.main
  (:require [nubank.workspaces.core :as ws]
            [nubank.workspaces.card-types.fulcro3 :as ct.fulcro]
            [rocks.mygiftlist.ui.gift-list :as gift]
            [rocks.mygiftlist.ui.root :as r]))

(ws/defcard gift-card
  (ct.fulcro/fulcro-card
   {::ct.fulcro/root gift/GiftList
    ::ct.fulcro/initial-state "Add more gifts"}))

(ws/defcard new-gift-card
  (ct.fulcro/fulcro-card
   {::ct.fulcro/root gift/GiftListForm}))

(ws/defcard root-card
  (ct.fulcro/fulcro-card
   {::ct.fulcro/root r/Root
    ::ct.fulcro/wrap-root? false}))

(defonce init (ws/mount))