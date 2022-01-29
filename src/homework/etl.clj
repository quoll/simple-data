(ns ^{:doc "Namespace for extracting, transforming, and loading data."
      :author "Paula Gearon"}
    homework.etl
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [homework.storage :as storage])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatter]))

(def ^:private separators [#" " #"," #"\|"])
(def ^:private fields [:last-name :first-name :email :favorite-color :dob])
(def ^:private record-length (count fields))
(def ^:private date-parser (DateTimeFormatter/ofPattern "MM/dd/yyyy"))
(def ^:private date-format (DateTimeFormatter/ofPattern "M/d/yyyy"))
(def ^{:private true
       :doc "a sequence of offset/function pairs for updating lexed field data into an appropriate type"}
  types [[4 #(LocalDate/from (.parse date-parser %))]])

(defn output-str
  "Converts a record into a string.
  record: a map containing the record to convert.
  return: a string representation of the record."
  [{:keys [last-name first-name email favorite-color dob] :as record}]
  (format "%s, %s, %s, %s, %s" last-name first-name email favorite-color (.format date-format dob)))

(defn update-types
  "Updates data in records to have appropriate types
  field-data: a vector of string fields to be converted into a record.
  returns: a vector of fields with some values parsed into require types."
  [field-data]
  (log/tracef "Processing lexed line: %s" (pr-str field-data))
  ;; for each type offset/fn pair, update the value at n with the function
  (try
    (reduce (fn [data [n converter]]
              (log/tracef "Updating type of column %d: %s" n (nth data n))
              (update data n converter))
            field-data types)
    (catch Exception e
      (log/errorf "Error parsing data on line: %s" (pr-str field-data))
      (throw (ex-info (format "Parse error in %s: %s" (pr-str field-data) (ex-message e))
                      {:cause e})))))

(defn select-reader
  "Reads a line of text to determine which record reader should be used.
  line: A string to be parsed as a record.
  returns: a function that maps a string to a record.
  throws an exception if no appropriate function may be found."
  [line]
  (log/debugf "Scanning line for appropriate parser: %s" line)
  (or
   ;; return a function for the first match
   (some #(when (= record-length (count (str/split line %)))
            (log/tracef "Found a matching separator: " (str %))
            ;; capture the matching separator in a function that builds the record 
            (fn [s]
              (->> (str/split s %)    ;; close over the separator and split by it
                   update-types       ;; update any fields that need specific types
                   (zipmap fields)))) ;; create a map, labeling all of the fields
         separators)
   (do
     (log/errorf "Unable to find a parser for: %s" line)
     (throw (ex-info "No matching parser found" {:text line})))))

(defn find-location
  "Converts a location into a loadable file.
  location: a string that may be a resource path or a file path.
  return: a url, file, or nil if the location cannot be resolved"
  [location]
  ;; look on the classpath for the file
  (or (io/resource location)
      ;; otherwise, see if the path can be resolved
      (let [f (io/file location)]
        (and (.exists f) f))))

(defn load-data
  "Loads records from a location, running it through a data transform.
  Throws an exception if a file cannot be found at the location.
  location: the location of a records file. May be a path or a URL.
  transform-to: a function that accepts a lazy sequence of records and
                returns the outcome of its operation.
  return: The result of the transform."
  [location transform-to]
  (if-let [loc (find-location location)]
    (with-open [rdr (io/reader loc)]
      (log/debugf "Found data at: %s" (str location))
      (let [[f :as lines] (line-seq rdr)
            record-tx (select-reader f)]
        (transform-to (map record-tx lines))))
    (do
      (log/errorf "Unable to locate file at: %s" location)
      (throw (ex-info "File not found" {:location location})))))

(defn load-records-into
  "Loads records from a location into a storage handle.
  Throws an exception if a file cannot be found at the location.
  location: the location of a records file. May be a path or a URL.
  storage: A handle to storage to insert the data into.
  return: The storage handle."
  [location storage]
  (letfn [(transform [records]
            ;; stream the records to storage
            (log/tracef "loading records")
            (storage/append! storage records))]
    (load-data location transform)))

(defn load-records
  "Eagerly loads records from a location.
  Throws an exception if a file cannot be found at the location.
  location: the location of a records file. May be a path or a URL.
  return: a sequence of records"
  [location]
  (load-data location #(into [] %)))  ;; map the data into a vector of records