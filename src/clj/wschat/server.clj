(ns wschat.server
  (:gen-class)
  (:require [org.httpkit.server :refer [with-channel
                                        on-receive
                                        send!
                                        on-close
                                        run-server]]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :refer [resources not-found]]))

(defonce channel-store (atom #{}))

(defn add-to-store [ch]
  (swap! channel-store conj ch))

(defn remove-from-store [ch]
  (swap! channel-store disj ch))

(defn broadcast [f]
  (doseq [ch @channel-store]
    (f ch)))

(defn ws-handler
  [req]
  (with-channel req ch
    (println "channel connected: " ch)
    (add-to-store ch)
    (on-receive ch (fn [data]
                     (broadcast #(send! % data))
                     (println "message received: " data)))
    (on-close ch (fn [status]
                   (remove-from-store ch)
                   (println "channel closed: " status)))))

(defn wrap-dir-index [handler]
  (fn [req]
    (handler
      (update-in req [:uri]
                 #(if (= "/" %) "/index.html" %)))))

(defroutes app-routes
  (GET "/ws" [] ws-handler)
  (resources "/")
  (not-found "Not found"))

(def app (-> app-routes
             wrap-dir-index))

(defonce server (atom nil))

(defn start-server
  []
  (reset! server (run-server #'app {:port 3000}))
  (println "server started."))

(defn stop-server
  []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)
    (println "server stopped.")))

(defn restart-server
  []
  (stop-server)
  (start-server))

(defn -main
  [& args]
  (start-server))
