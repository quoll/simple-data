(ns homework.etl-test
  (:require [clojure.test :refer :all]
            [homework.etl :refer [output-str update-types select-reader find-location
                                  load-data load-records-into load-records]])
  (:import [java.time LocalDate]
           [clojure.lang ExceptionInfo]))

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
      (is (.exists f)))))

(deftest load-data-test
  )

(deftest load-records-into-test
  )

(deftest load-records-test
  )
