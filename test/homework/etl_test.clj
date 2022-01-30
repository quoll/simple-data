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
