(ns rocks.mygiftlist.ui.gift-list
  (:require
   [com.fulcrologic.fulcro.algorithms.form-state :as fs]
   [com.fulcrologic.fulcro.algorithms.merge :as merge]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.dom :as dom]
   [com.fulcrologic.fulcro.mutations :as m]

   [com.fulcrologic.semantic-ui.elements.button.ui-button :refer [ui-button]]
   [com.fulcrologic.semantic-ui.collections.form.ui-form :refer [ui-form]]
   [com.fulcrologic.semantic-ui.collections.form.ui-form-input :refer [ui-form-input]]

   [rocks.mygiftlist.type.gift-list :as gift-list]
   [rocks.mygiftlist.model.gift-list :as m.gift-list]))

(declare GiftListForm)

(defn- pristine-gift-list-form-state []
  (fs/add-form-config
    GiftListForm
    #::gift-list {:id (random-uuid)
                  :name ""}))

(defsc GiftListForm [this
                     {::gift-list/keys [name] :as gift-list}
                     {:keys [reset-form!]}]
  {:ident ::gift-list/id
   :query [::gift-list/id ::gift-list/name fs/form-config-join]
   :initial-state (fn [_] (pristine-gift-list-form-state))
   :form-fields #{::gift-list/name}}
  (let [validity (fs/get-spec-validity gift-list ::gift-list/name)]
    (ui-form
      {:onSubmit (fn [_]
                   (if-not (= :valid validity)
                     (comp/transact! this [(fs/mark-complete! {})])
                     (do
                       (comp/transact! this
                         [(m.gift-list/create-gift-list
                            (select-keys gift-list
                              [::gift-list/id ::gift-list/name]))])
                       (reset-form!))))}
      (ui-form-input
        {:placeholder "Birthday 2020"
         :onChange (fn [evt]
                     (m/set-string! this ::gift-list/name :event evt))
         :onBlur (fn [_]
                   (comp/transact! this
                     [(fs/mark-complete! {:field ::gift-list/name})]))
         :error (when (= :invalid validity)
                  "Gift list name cannot be blank")
         :fluid true
         :value name})
      (ui-button
        {:type "submit"
         :primary true
         :disabled (= :invalid validity)}
        "Submit"))))

(def ui-gift-list-form (comp/factory GiftListForm))

(defsc GiftListFormPanel [this {:ui/keys [gift-list-form]}]
  {:ident (fn [] [:component/id :gift-list-form-panel])
   :query [{:ui/gift-list-form (comp/get-query GiftListForm)}]
   :initial-state {:ui/gift-list-form {}}}
  (dom/div {}
    (ui-gift-list-form
      (comp/computed gift-list-form
        {:reset-form! #(merge/merge-component! this GiftListFormPanel
                         {:ui/gift-list-form
                          (pristine-gift-list-form-state)})}))))

(def ui-gift-list-form-panel (comp/factory GiftListFormPanel))

(defsc CreatedGiftList [_this {::gift-list/keys [name]}]
  {:ident ::gift-list/id
   :query [::gift-list/id ::gift-list/name]}
  (dom/li {} name))

(def ui-created-gift-list
  (comp/factory CreatedGiftList {:keyfn ::gift-list/id}))

(defsc CreatedGiftLists [_this {:keys [created-gift-lists]}]
  {:ident (fn [] [:component/id :created-gift-lists])
   :query [{:created-gift-lists (comp/get-query CreatedGiftList)}]
   :initial-state {:created-gift-lists []}}
  (dom/ul {}
    (mapv ui-created-gift-list created-gift-lists)))

(def ui-created-gift-lists (comp/factory CreatedGiftLists))