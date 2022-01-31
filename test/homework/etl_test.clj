(ns homework.etl-test
  (:require [clojure.test :refer :all]
            [homework.etl :refer [date-str output-str update-types select-reader find-location
                                  load-data load-records-into load-records]])
  (:import [java.time LocalDate]
           [clojure.lang ExceptionInfo]))

(deftest date-str-test
  (testing "Converting date to a string"
    (is (= "1/30/2022" (date-str (LocalDate/of 2022 1 30)))))
  (testing "Converting nil date to string"
    (is (= "" (date-str nil)))))

(deftest output-str-test
  (testing "Testing record conversion to string."
    (is (= "Lovelace, Ada, ada@analytical.co.uk, blue, 12/10/1815"
           (output-str {:last-name "Lovelace"
                        :first-name "Ada"
                        :email "ada@analytical.co.uk"
                        :favorite-color "blue"
                        :dob (LocalDate/of 1815 12 10)})))
    (testing "Records with extra fields"
      (is (= "Lovelace, Ada, ada@analytical.co.uk, blue, 12/10/1815"
             (output-str {:last-name "Lovelace"
                          :first-name "Ada"
                          :email "ada@analytical.co.uk"
                          :favorite-color "blue"
                          :dob (LocalDate/of 1815 12 10)
                          :dod (LocalDate/of 1852 11 27)}))))
    (testing "Records with missing fields"
      (is (= "Lovelace, Ada, ada@analytical.co.uk, , "
             (output-str {:last-name "Lovelace"
                          :first-name "Ada"
                          :email "ada@analytical.co.uk"}))))))

(def parse-long* #(Long/parseLong %))
(def parse-hex #(Long/parseLong % 16))

(deftest update-types-test
  (testing "Updates to data"
    (let [raw-data ["0" "10" "12" "30" "24"]]
      (is (= ["0" "10" "12" "30" "24"])
          (update-types [] raw-data))
      (is (= ["0" 10 "12" "30" "24"])
          (update-types [[1 parse-long*]] raw-data))
      (is (= ["0" 10 18 "30" "24"])
          (update-types [[1 parse-long*] [2 parse-hex]] raw-data))
      (is (= ["0" 10 18 "30" 36])
          (update-types [[4 parse-hex] [1 parse-long*] [0 parse-long*] [2 parse-hex]] raw-data))))
  (testing "Checking for parse errors"
    (is (thrown-with-msg? ExceptionInfo #"^Parse error in .* For input string: \"one\""
                          (update-types [[1 parse-long*]] ["0" "one" "2"])))))

(deftest select-reader-test
  (let [expected {:last-name "Darcy"
                  :first-name "Fitzwilliam"
                  :email "bill@pemberly.com"
                  :favorite-color "red"
                  :dob (LocalDate/of 1806 1 28)}]
    (let [rdrc (select-reader "Bennet,Elizabeth,liz@longbourn.com,blue,1/28/1813")
          rdrc2 (select-reader "Bennet, Elizabeth, liz@longbourn.com, blue, 1/28/1813")]
      (testing "Comma separated strings are parsed by the selected reader, even when it is possible to space-separate."
        (is (= expected (rdrc "Darcy,Fitzwilliam,bill@pemberly.com,red,1/28/1806")))
        (is (= expected (rdrc2 "Darcy,Fitzwilliam,bill@pemberly.com,red,1/28/1806"))))
      (testing "Selected parser accepts 0-padded dates."
        (is (= expected (rdrc "Darcy,Fitzwilliam,bill@pemberly.com,red,01/28/1806")))
        (is (= expected (rdrc2 "Darcy,Fitzwilliam,bill@pemberly.com,red,01/28/1806")))))
      
    (let [rdrc (select-reader "Bennet|Elizabeth|liz@longbourn.com|blue|1/28/1813")
          rdrc2 (select-reader "Bennet | Elizabeth | liz@longbourn.com | blue | 1/28/1813")]
      (testing "Pipe separated strings are parsed by the selected reader, even when it is possible to space-separate."
        (is (= expected (rdrc "Darcy|Fitzwilliam|bill@pemberly.com|red|1/28/1806")))
        (is (= expected (rdrc2 "Darcy|Fitzwilliam|bill@pemberly.com|red|1/28/1806"))))
      (testing "Selected parser accepts 0-padded dates."
        (is (= expected (rdrc "Darcy|Fitzwilliam|bill@pemberly.com|red|01/28/1806")))
        (is (= expected (rdrc2 "Darcy|Fitzwilliam|bill@pemberly.com|red|01/28/1806"))))
      (testing "Selected reader keeps extra spaces for padding, but ignores when parsing dates."
        (let [expected {:last-name "Darcy "
                        :first-name " Fitzwilliam "
                        :email " bill@pemberly.com "
                        :favorite-color " red "
                        :dob (LocalDate/of 1806 1 28)}]
          (is (= expected (rdrc "Darcy | Fitzwilliam | bill@pemberly.com | red | 01/28/1806"))))))

    (let [rdrc (select-reader "Bennet Elizabeth liz@longbourn.com blue 1/28/1813")]
      (testing "Comma separated strings are parsed by the selected reader."
        (is (= expected (rdrc "Darcy Fitzwilliam bill@pemberly.com red 1/28/1806"))))))

  (testing "Fail to select a reader when insufficient fields are found"
    (is (thrown-with-msg? ExceptionInfo #"No matching parser found"
                          (select-reader "")))
    (is (thrown-with-msg? ExceptionInfo #"No matching parser found"
                          (select-reader "Bennet,Elizabeth,liz@longbourn.com,1/28/1813"))))
  (testing "Fail to select a reader when mixing separator types"
    (is (thrown-with-msg? ExceptionInfo #"No matching parser found"
                          (select-reader "Bennet,Elizabeth,liz@longbourn.com|email|1/28/1813")))))

(deftest find-location-test
  (testing "Find a file in the resources"
    (let [url (find-location "data.csv")]
      (is (some? url))
      (is (= "file" (.getProtocol url)))))
  (testing "Find a file on disk"
    (let [f (find-location "data.ssv")]
      (is (.exists f))))
  (testing "Look for something that doesn't exist"
    (is (not (find-location "non-existent.txt")))))

(deftest load-data-test
  (testing "Loaded data is presented to the callback."
    (let [record-keys (set @#'homework.etl/fields)
          handle (load-data "data.csv"
                            (fn [records]
                              (testing "Lazy sequence."
                                (is (not (counted? records))))
                              (let [record-count (volatile! 0)]
                                (doseq [record records]
                                  (vswap! record-count inc)
                                  (is (= record-keys (set (keys record)))))
                                (is (= @record-count 6)))
                              :handle))]
      (testing "Is returned value to the same data as stored."
        (is (= :handle handle)))))
  (testing "Missing file throws exception"
    (is (thrown-with-msg? ExceptionInfo #"File not found" (load-data "missing.csv" identity)))))

(deftest load-records-into-test
  (let [store {:label "test" :data (atom [])}]
    (testing "Storage is filled, then returned."
      (let [{:keys [label data]} (load-records-into "data.csv" store)]
        (is (= label "test"))
        (is (= 6 (count @(:data store))))
        (testing "Loading the same data again continues to fill the store.")
        (load-records-into "data.ssv" store)
        (is (= 12 (count @(:data store))))
        (let [first6 (take 6 @(:data store))
              second6 (drop 6 @(:data store))]
          (doseq [[a b] (map vector first6 second6)] (is (= a b))))))
    (testing "Missing file throws exception"
      (is (thrown-with-msg? ExceptionInfo #"File not found"
                            (load-records-into "missing.csv" store))))))

(deftest load-records-test
  (testing "Storage is filled from different locations, then returned."
    (let [data (load-records "data.csv")
          data2 (load-records "data.ssv")]
      (is (= 6 (count data)))
      (is (= 6 (count data2)))
      (is (= data data2))))
  (testing "Missing file throws exception"
    (is (thrown-with-msg? ExceptionInfo #"File not found"
                          (load-records "missing.csv")))))
