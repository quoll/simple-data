(ns ^{:doc "This namespace handles storage and retrieval of data for the app.
            No database is used, so it is a simple stub that stores data in an atom."
      :author "Paula Gearon"}
    homework.storage
    (:require [clojure.tools.logging :as log]))

(defn create-storage
  "Connects to storage and initializes the connection according to the argument.
  Returns a handle to the storage."
  [arg]
  {:label arg
   :data (atom [])})

(defn append!
  "Appends data to a storage instance.
  store: The handle to the storage instance.
  records: A sequence of records to be stored.
  returns: The storage handle, possibly update (not in this implementation)."
  [{:keys [data label] :as store} records]
  (log/debugf "Saving data to storage: %s" label)
  (if (sequential? records)
    (do
      (log/debugf "Storing %d records" (count records))
      (swap! data into records)
      store)
    (do
      (log/debugf "Unexpected data type added to storage %s" (str (type records)))
      (throw (ex-info "Bad data appended to storage" {:data-type (type records)})))))

(defn compare-for
  "Returns a comparison function based on file/direction pairs.
  orderings: pairs of field/direction in a flat sequence.
             direction must be :asc for ascending or :desc for descending.
  return: a comparison function"
  [orderings]
  {:pre [(zero? (mod (count orderings) 2))]}
  (if-not (seq orderings)
    ;; no orderings provided, maps cannot be sorted, so sort by their string representations
    (fn [a b] (compare (str a) (str b)))
    (let [ords (partition 2 orderings)]
      (fn [a b]
        (log/tracef "Comparing %s / %s" (pr-str a) (pr-str b))
        (loop [[[field dir :as o] & remaining] ords]
          (log/tracef "Comparing by %s (%s)" (str field) (name dir))
          (if-not o
            (compare (str a) (str b))  ;; no more fields to sort by. Return string compare for stable sorting
            (let [fa (get a field)
                  fb (get b field)
                  c (compare fa fb)]
              (if (zero? c)
                (recur remaining)  ;; fields compare the same, so move to the next ordering
                (if (= dir :desc) (- c) c)))))))))

(defn retrieve
  "Retrieves data from storage.
  store: The handle to the storage instance.
  returns: A seq of records from storage."
  [{:keys [data label] :as store} & ordering]
  (log/debugf "Reading from: %s" label)
  (let [sorting-fn (compare-for ordering)]
    (sort sorting-fn @data)))
