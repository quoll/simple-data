(ns ^{:doc "Functions to provide an HTTP service"
      :author "Paula Gearon"}
    homework.service
    (:require [clojure.tools.logging :as log]
              [clojure.tools.cli :as cli]
              [ring.adapter.jetty :as jetty]
              [homework.storage :as storage]
              [homework.routes :as routes])
    (:import [java.net BindException]))

(def options
  [["-p" "--port PORT" "HTTP port"
    :default 8080
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-s" "--storage STORE" "storage name"
    :default "SERVICE"]])

(defn start-server
  "Creates a listener that will route requests to the provided port"
  [store-name port]
  (let [storage (storage/create-storage store-name)
        app (routes/create-app storage)]
    (log/debugf "Starting Jetty server")
    (jetty/run-jetty app {:port port})))

(defn -main
  "Entry point for the HTTP service program."
  [& args]
  (let [{{:keys [port storage]} :options :as opts} (cli/parse-opts args options)]
    (log/infof "Starting service on port %d" port)
    (log/debugf "Storage label: %s" storage)
    (try
      (start-server storage port)
      (catch BindException e
        (log/error "Server port already in use")))))
