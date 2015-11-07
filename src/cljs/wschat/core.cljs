(ns wschat.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent.ratom :refer [reaction]])
  (:require [reagent.core :refer [render dom-node]]
            [reagent.ratom :refer [atom]]
            [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [chan <! >! put! close!]]
            [re-frame.core :refer [register-handler register-sub dispatch subscribe]]))

(register-handler
  :initialize
  (fn [_ _] {:messages []}))

(register-handler
  :connect-server
  (fn [db [_ server-ch]]
    (go-loop []
      (let [{:keys [message error]} (<! server-ch)]
        (cond error (dispatch [:error error])
              (nil? message) (dispatch [:connection-closed])
              :else (dispatch [:receive-message message]))
        (when message
          (recur))))
    (assoc db :server-ch server-ch)))

(register-handler
  :connection-closed
  (fn [db _]
    (dissoc db :server-ch)))

(register-handler
  :receive-message
  (fn [db [_ message]]
    #_(.log js/console (str "Received: " message))
    (update db :messages (comp (partial take 10) conj) message)))

(register-handler
  :send-message
  (fn [db [_ message]]
    #_(.log js/console (str "Send: " message))
    (when-let [server-ch (:server-ch db)]
      (go (>! server-ch {:datetime (js/Date.)
                         :message message})))
    db))

(register-sub
  :server-connected?
  (fn [db]
    (reaction (contains? @db :server-ch))))

(register-sub
  :messages
  (fn [db]
    (reaction (:messages @db))))

(defn format-datetime [datetime]
  (letfn [(zero-pad
            ([s] (zero-pad s 2))
            ([s n] (.slice (str "0" s) (- n))))]
    (let [full-year (-> datetime .getFullYear (zero-pad 4))
          month (-> datetime .getMonth inc zero-pad)
          date (-> datetime .getDate zero-pad)
          day-tbl ["日" "月" "火" "水" "木" "金" "土"]
          day (->> datetime .getDay (nth day-tbl))
          hour (-> datetime .getHours zero-pad)
          minute (-> datetime .getMinutes zero-pad)
          sec (-> datetime .getSeconds zero-pad)]
      (str full-year "/" month "/" date "(" day ")" hour "時" minute "分" sec "秒"))))

(defn error-panel [error]
  [:div "Error: " error])

(defn message-edit [message]
  (with-meta [:input {:type "text"
                      :value @message
                      :placeholder "Input your message"
                      :on-change #(reset! message (-> % .-target .-value))}]
             {:component-did-mount
              #(fn [_] (.log js/console "hi"))}))

(def initial-focus
  (with-meta identity
             {:component-did-mount #(.focus (dom-node %))}))

(defn input-panel []
  (let [message (atom nil)]
    (fn []
      [:form {:on-submit (fn [e]
                           (.preventDefault e)
                           (when (seq @message)
                             (dispatch [:send-message @message])
                             (reset! message nil)))}
       [initial-focus [:input {:type "text"
                               :value @message
                               :placeholder "Input your message"
                               :on-change #(reset! message (-> % .-target .-value))}]]
       [:input {:type "submit"
                :value "Send"
                :disabled (empty? @message)}]])))

(defn message-panel []
  (let [ms (subscribe [:messages])]
    (fn []
      (when-not (empty? @ms)
        [:ol.messages
         (for [[idx {:keys [datetime message]}] (map-indexed vector @ms)]
           ^{:key (str "message-" idx)}
           [:li
            [:span.datetime (format-datetime datetime)]
            [:span.message message]])]))))

(defn main-panel []
  [:div
   [input-panel]
   [message-panel]])

(defn render-panel [panel]
  (render panel (.getElementById js/document "app")))

(defn mount-root []
  (go
    (let [{:keys [ws-channel error]} (<! (ws-ch "ws://localhost:3000/ws"))]
      (if error
        (render-panel [error-panel error])
        (do (dispatch [:connect-server ws-channel])
            (render-panel [main-panel]))))))

(defn ^:export init []
  (mount-root))
