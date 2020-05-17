(ns rocks.mygiftlist.ui.root
  (:require
   [rocks.mygiftlist.authentication :as auth]
   [rocks.mygiftlist.ui.navigation :as ui.nav]
   [rocks.mygiftlist.ui.gift-list :as ui.gift-list]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.data-fetch :as df]
   [com.fulcrologic.fulcro.dom :as dom]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]))

(defsc LoginForm [_this _]
  {:query []
   :ident (fn [] [:component/id :login])
   :route-segment ["login"]
   :initial-state {}}
  (dom/div {}
    (dom/div "In order to view and create gift lists, you need to...")
    (dom/div (dom/button :.ui.primary.button
               {:onClick #(auth/login)}
               "Log in or sign up"))))

(defsc Home [_this {:ui/keys [gift-list-form-panel created-gift-lists]}]
  {:query [{:ui/gift-list-form-panel (comp/get-query ui.gift-list/GiftListFormPanel)}
           {:ui/created-gift-lists (comp/get-query ui.gift-list/CreatedGiftLists)}]
   :ident (fn [] [:component/id :home])
   :initial-state {:ui/gift-list-form-panel {}
                   :ui/created-gift-lists {}}
   :route-segment ["home"]
   :will-enter (fn [app _]
                 (df/load! app [:component/id :created-gift-lists]
                   ui.gift-list/CreatedGiftLists)
                 (dr/route-immediate [:component/id :home]))}
  (dom/div {}
    (dom/h3 {} "Home Screen")
    (dom/div {} "Just getting started? Create a new gift list!")
    (ui.gift-list/ui-gift-list-form-panel gift-list-form-panel)
    (ui.gift-list/ui-created-gift-lists created-gift-lists)))

(defsc About [_this _]
  {:query []
   :ident (fn [] [:component/id :home])
   :initial-state {}
   :route-segment ["about"]}
  (dom/div {}
    (dom/h3 {} "About My Gift List")
    (dom/div {} "It's a really cool app!")))

(defn loading-spinner []
  (dom/div :.ui.active.inverted.dimmer
    (dom/div :.ui.loader)))

(defsc Loading [_this _]
  {:query []
   :ident (fn [] [:component/id ::loading])
   :initial-state {}
   :route-segment ["loading"]}
  (loading-spinner))

(defrouter MainRouter [_ _]
  {:router-targets [Loading LoginForm Home About]}
  (loading-spinner))

(def ui-main-router (comp/factory MainRouter))

(defsc Root [_this {:root/keys [router navbar loading]}]
  {:query [{:root/router (comp/get-query MainRouter)}
           {:root/navbar (comp/get-query ui.nav/Navbar)}
           :root/loading]
   :initial-state {:root/router {}
                   :root/navbar {}
                   :root/loading true}}
  (if loading
    (loading-spinner)
    (dom/div {}
      (ui.nav/ui-navbar navbar)
      (dom/div :.ui.container
        (ui-main-router router)))))
