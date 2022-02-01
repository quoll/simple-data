(ns homework.routes-test
  (:require [clojure.test :refer [deftest testing is]]
            [peridot.core :refer [session request]]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [homework.storage :refer [create-storage]]
            [homework.routes :refer [create-app]]
            [homework.etl :as etl]))

(defn extract-data
  "Utility to extract the JSON data from a session.
  session: the application session.
  return: The decoded data."
  [{{body :body} :response :as session}]
  (json/read-str body :key-fn keyword))

(defn check-empty
  "Utility to check a peridot session for an empty JSON response.
  session: The application session.
  return: the session."
  [session]
  (is (= [] (extract-data session)))
  session)

(defn check-count
  "Checks the peridot session for the count of the response.
  session: the session to query.
  n: the number of elements expected in the response."
  [session n]
  (is (= n (count (extract-data session))))
  session)

(defn get-data
  "Performs a GET operation against a peridot session.
  session: The session to perform the GET against.
  record-type: The type of record being requested."
  [session record-type]
  (request session (str "/records/" record-type)
           :headers {"host" "localhost" "accept" "application/json"}))

(defn post-line
  "Performs at POST operation against a peridot session,
  sending the provided text.
  session: the session to post to.
  text: the body of the post.
  return: The session after the post."
  [session text]
  (request session "/records"
           :request-method :post
           :content-type "text/plain"
           :body text))

(deftest empty-test
  (testing "Empty storage returns an empty JSON array"
    (-> (session (create-app (create-storage "TEST")))
        (get-data "name")
        check-empty
        (get-data "birthdate")
        check-empty
        (get-data "name")
        check-empty)))

(defn load-data
  "Loads external data lines into vector"
  [ext]
  (-> (str "data." ext)
      (#(or (io/resource %) %))  ;; picks up some files from the classpath
                                 ;; others in the current directory
      slurp
      (str/split #"\n")))

(def csv-data (load-data "csv"))
(def psv-data (load-data "psv"))
(def ssv-data (load-data "ssv"))
;; mix the data types into a vector
(def mix-data (-> []
                  (into (take 2 csv-data))          ;; first two comma separated
                  (into (take 2 (drop 2 psv-data))) ;; next two pipe separated
                  (into (drop 4 ssv-data))))        ;; last two space separated

(deftest post-test
  (testing "Adding data expands the storage."
    ;; initialize the session and check that it is "successfully empty"
    (let [sess (-> (session (create-app (create-storage "TEST")))
                   (get-data "name")
                   check-empty)]
      ;; load into the session, and check at each step if the stored count went up
      (reduce-kv (fn [s n line]
                   (-> s
                       (post-line line)
                       (get-data "name")
                       (check-count (inc n)))
                   s)
                 sess
                 mix-data))))

(defn reorder
  "Reorders a vector according to the provided indexes.
  v: The original vector.
  idxs: offsets of the vector to insert into the output, in order."
  [v & idxs]
  (mapv v idxs))

(deftest sort-test
  (testing "Data is sorted correctly for each endpoint."
    ;; initialize the session, using a mix of file types
    (let [;; create the session
          empty-sess (session (create-app (create-storage "TEST")))
          ;; load the data
          sess (reduce (fn [s line] (post-line s line)) empty-sess mix-data)
          sess (get-data sess "color")
          color-data (extract-data sess)
          sess (get-data sess "name")
          name-data (extract-data sess)
          sess (get-data sess "birthdate")
          birthdate-data (extract-data sess)
          processed-input (->> csv-data
                               (map #((etl/select-reader %) %))
                               (mapv #(update % :dob etl/date-str)))]
      (testing "Ordered by favorite color."
        (is (= (reorder processed-input 0 5 3 2 1 4) color-data)))
      (testing "Ordered by last name."
        (is (= (reorder processed-input 0 5 3 1 2 4) name-data)))
      (testing "Ordered by birthdate."
        (is (= (reorder processed-input 1 2 3 4 5 0) birthdate-data))))))
