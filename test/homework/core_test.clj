(ns homework.core-test
  (:require [clojure.test :refer :all]
            [homework.core :refer [cli-output]]
            [homework.etl :as etl]))

(def test-data {:label "test"
                :data (atom nil)})

(def ordered-by-color-lname
  "Bennet, Elizabeth, liz@longbourn.com, blue, 1/28/1813
Bennet, Jane, jane@longbourne.com, blue, 6/5/1811
Bingley, Charles, chuck@netherfield.co.uk, blue, 7/10/1807
Lucas, Charlotte, charlotte@hunsford.org, lavendar, 8/22/1806
Darcy, Fitzwilliam, bill@pemberley.com, red, 1/28/1806
Wickham, George, george@cad.com, red, 3/19/1808")

(def ordered-by-dob
  "Darcy, Fitzwilliam, bill@pemberley.com, red, 1/28/1806
Lucas, Charlotte, charlotte@hunsford.org, lavendar, 8/22/1806
Bingley, Charles, chuck@netherfield.co.uk, blue, 7/10/1807
Wickham, George, george@cad.com, red, 3/19/1808
Bennet, Jane, jane@longbourne.com, blue, 6/5/1811
Bennet, Elizabeth, liz@longbourn.com, blue, 1/28/1813")

(def ordered-by-lname-desc
  "Wickham, George, george@cad.com, red, 3/19/1808
Lucas, Charlotte, charlotte@hunsford.org, lavendar, 8/22/1806
Darcy, Fitzwilliam, bill@pemberley.com, red, 1/28/1806
Bingley, Charles, chuck@netherfield.co.uk, blue, 7/10/1807
Bennet, Elizabeth, liz@longbourn.com, blue, 1/28/1813
Bennet, Jane, jane@longbourne.com, blue, 6/5/1811")

(deftest user-output-test
  (testing "Output is as expected."
    (testing "Empty output follows template pattern."
      (reset! (:data test-data) [])
      (let [output (with-out-str (cli-output test-data))]
        (is (= output
               (str "\nOutput 1: by favorite color/last-name\n"
                    "\nOutput 2: by birth date\n"
                    "\nOutput 3: by last-name (descending)\n\n")))))
    (testing "Provided data is ordered as expected."
      (reset! (:data test-data) (etl/load-records "data.csv"))
      (let [output (with-out-str (cli-output test-data))]
        (is (= output
               (str "\nOutput 1: by favorite color/last-name\n"
                    ordered-by-color-lname
                    "\n\nOutput 2: by birth date\n"
                    ordered-by-dob
                    "\n\nOutput 3: by last-name (descending)\n"
                    ordered-by-lname-desc "\n\n")))))))
