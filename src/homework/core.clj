(ns homework.core
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [clojure.tools.cli :as cli]
            [clojure.pprint :as pprint]
            [homework.storage :as storage]
            [homework.etl :as etl]))

(def ^:const default-file "data.csv")

(def options
  [["-s" "--store STORE" "Storage connector"
    :default "STORAGE"]
   ["-f" "--file FILE" "path or URL of file"
    :default default-file]])

(defn user-output
  "Prints the stored data to standard out, using 3 different types of sorting.
  storage: The handle to storage.
  returns: nil"
  [storage]
  (println "\nOutput 1: by favorite color/last-name")
  (doseq [record (storage/retrieve storage :favorite-color :asc :last-name :asc)]
    (println (etl/output-str record)))
  (println "\nOutput 2: by birth date")
  (doseq [record (storage/retrieve storage :dob :asc)]
    (println (etl/output-str record)))
  (println "\nOutput 3: by last-name (descending)")
  (doseq [record (storage/retrieve storage :last-name :desc)]
    (println (etl/output-str record)))
  (println))

(defn -main
  "Initialize and launch the service."
  [& args]
  (let [{{:keys [store file]} :options} (cli/parse-opts args options)
        storage (storage/create-storage store)]
    (log/infof "Loading data from: %s" file)
    (etl/load-records-into file storage)
    (log/info "Reading back data")
    (user-output storage)
    ;; (log/info "Starting service...")
    (log/info "Exiting")))
