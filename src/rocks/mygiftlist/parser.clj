(ns rocks.mygiftlist.parser
  (:require
   [taoensso.timbre :as log]
   [integrant.core :as ig]
   [com.wsscode.pathom.connect :as pc :refer [defresolver]]
   [com.wsscode.pathom.core :as p]
   [clojure.core.async :refer [<!!]]
   [rocks.mygiftlist.authentication :as auth]
   [rocks.mygiftlist.db :as db]
   [rocks.mygiftlist.type.user :as user]
   [rocks.mygiftlist.model.gift-list :as m.gift-list]
   [rocks.mygiftlist.model.user :as m.user]))

(defresolver index-explorer [env _]
  {::pc/input  #{:com.wsscode.pathom.viz.index-explorer/id}
   ::pc/output [:com.wsscode.pathom.viz.index-explorer/index]}
  {:com.wsscode.pathom.viz.index-explorer/index
   (-> (get env ::pc/indexes)
     (update ::pc/index-resolvers
       #(into {} (map (fn [[k v]] [k (dissoc v ::pc/resolve)])) %))
     (update ::pc/index-mutations
       #(into {} (map (fn [[k v]] [k (dissoc v ::pc/mutate)])) %)))})

(def all-resolvers [index-explorer
                    m.user/user-resolvers
                    m.gift-list/gift-list-resolvers])

(defn preprocess-parser-plugin
  "Helper to create a plugin that can view/modify the env/tx of a
  top-level request.
  f - (fn [{:keys [env tx]}] {:env new-env :tx new-tx})
  If the function returns no env or tx, then the parser will not be
  called (aborts the parse)"
  [f]
  {::p/wrap-parser
   (fn transform-parser-out-plugin-external [parser]
     (fn transform-parser-out-plugin-internal [env tx]
       (let [{:keys [env tx]} (f {:env env :tx tx})]
         (if (and (map? env) (seq tx))
           (parser env tx)
           {}))))})

(defn log-requests [{:keys [env tx] :as req}]
  (log/debug "Pathom transaction:" (pr-str tx))
  (log/debug "Claims:" (-> env :ring/request ::auth/claims))
  req)

(defmethod ig/init-key ::parser
  [_ {::db/keys [pool]}]
  (let [real-parser
        (p/parallel-parser
          {::p/mutate  pc/mutate-async
           ::p/env     {::p/reader               [p/map-reader
                                                  pc/parallel-reader
                                                  pc/open-ident-reader
                                                  p/env-placeholder-reader]
                        ::p/placeholder-prefixes #{">"}}
           ::p/plugins [(pc/connect-plugin {::pc/register all-resolvers})
                        (p/env-wrap-plugin
                          (fn [env]
                            ;; Here is where you can dynamically add
                            ;; things to the resolver/mutation
                            ;; environment, like the server config,
                            ;; database connections, etc.
                            (let [requester-auth0-id (get-in env [:ring/request ::auth/claims :sub])]
                              (assoc env
                                ::db/pool pool
                                :requester-auth0-id requester-auth0-id
                                :requester-id
                                (and requester-auth0-id
                                  (::user/id (db/execute-one! pool
                                               {:select [:id]
                                                :from [:user]
                                                :where [:= requester-auth0-id :auth0_id]})))))))
                        (preprocess-parser-plugin log-requests)
                        p/error-handler-plugin
                        p/request-cache-plugin
                        (p/post-process-parser-plugin p/elide-not-found)
                        p/trace-plugin]})
        ;; NOTE: Add -Dtrace to the server JVM to enable Fulcro
        ;; Inspect query performance traces to the network tab.
        ;; Understand that this makes the network responses much
        ;; larger and should not be used in production.
        trace? (some? (System/getProperty "trace"))]
    (fn wrapped-parser [env tx]
      (<!! (real-parser env (if trace?
                              (conj tx :com.wsscode.pathom/trace)
                              tx))))))

(comment
  (require '[integrant.repl.state :refer [system]])
  (def parser (::parser system))
  (parser {:ring/request {::auth/claims {:sub "auth0|5e6da6bd10885b0ca6a93f4c"}}}
    [{[::user/id #uuid "dc623810-d2ba-49d4-b096-4e43dcefcbcf"]
      [::user/id ::user/auth0-id ::user/email]}])
  )
