(ns homework.storage-test
  (:require [clojure.test :refer :all]
            [homework.storage :refer [create-storage append! compare-for retrieve]]))

(deftest create-test
  (testing "Storage creation, and checking internal private structure"
    (let [{:keys [label data] :as s} (create-storage "test-data")]
      (is (= #{:label :data} (set (keys s))))
      (is (= "test-data" label))
      (is (vector? @data))
      (is (empty? @data)))))

(deftest append-test
  (let [s (create-storage "test-append")]
    (testing "Testing append operation"
      (let [data (deref (:data s))
            s2 (append! s [{:record "one"}])
            data2 (deref (:data s2))]
        ;; this implementation does not expect the handle to change
        (is (= s s2))
        ;; the data should have been changed
        (is (not= data data2))
        (is (= [{:record "one"}] data2))
        (let [s3 (append! s2 [{:record "two"}])]
          (is (= s3 s))
          (is (= [{:record "one"} {:record "two"}] (deref (:data s3)))))))
    (testing "Testing invalid append"
      (try
        (append! s {:record "three"})
        (is false "Accepted invalid data")
        (catch Exception _)))))

(def opposite
  "Map numeric comparison operators to their opposite operator"
  {> < < >})

(defn ordering-by-field
  "Helper function to test ordering comparisons on the field :b.
  c: The comparison function
  ord-g: the ordering test for greater values "
  [c ord-g]
  (let [ord-l (opposite ord-g)]
    (is (= 0 (c {:a 1 :b 1} {:a 1 :b 1})))
    ;; basic less-than
    (is (ord-g 0 (c {:a 1 :b 1} {:a 1 :b 2})))
    ;; less-than when other fields are greater
    (is (ord-g 0 (c {:a 2 :b 1} {:a 1 :b 2})))
    ;; basic greater-than
    (is (ord-l 0 (c {:a 1 :b 2} {:a 1 :b 1})))
    ;; greater-than when other fields are less
    (is (ord-l 0 (c {:a 1 :b 2} {:a 2 :b 1})))
    ;; string comparison when fields are equal.
    ;; Alphabetical ordering regardless of ascent/descent
    (is (> 0 (c {:a "a" :b 1} {:a "b" :b 1})))
    (is (< 0 (c {:a "b" :b 1} {:a "a" :b 1})))
    (testing "Ordering when the requested field is not present"
      (is (= 0 (c {:a 1} {:a 1})))
      (is (> 0 (c {:a 1} {:a 2})))
      (is (< 0 (c {:a 2} {:a 1}))))))

(defn ordering-by-2-fields
  "Helper function to test ordering comparisons on a pair of fields :b :c"
  [c ord-g1 ord-g2]
  (let [ord-l1 (opposite ord-g1)
        ord-l2 (opposite ord-g2)]
    (is (= 0 (c {:a 1 :b 1 :c 1} {:a 1 :b 1 :c 1})))
    ;; test alphabetical when required fields are equal
    (is (< 0 (c {:a 2 :b 1 :c 1} {:a 1 :b 1 :c 1})))
    (is (> 0 (c {:a 1 :b 1 :c 1} {:a 2 :b 1 :c 1})))
    ;; test on the first field, second field contradicting
    (is (ord-g1 0 (c {:a 1 :b 2 :c 1} {:a 1 :b 1 :c 2})))
    (is (ord-l1 0 (c {:a 1 :b 1 :c 2} {:a 1 :b 2 :c 1})))
    ;; test on the second field
    (is (ord-g2 0 (c {:a 1 :b 2 :c 2} {:a 1 :b 2 :c 1})))
    (is (ord-l2 0 (c {:a 1 :b 2 :c 1} {:a 1 :b 2 :c 2})))
    ;; test on the second field when the first is missing
    (is (ord-g2 0 (c {:a 1 :c 2} {:a 1 :c 1})))
    (is (ord-l2 0 (c {:a 1 :c 1} {:a 1 :c 2})))
    (testing "Ordering when the requested field is not present."
      (is (= 0 (c {:a 1} {:a 1})))
      (is (> 0 (c {:a 1} {:a 2})))
      (is (< 0 (c {:a 2} {:a 1}))))))

(deftest compare-for-test
  (testing "Asking for no comparison provides a valid record comparator."
    (let [c (compare-for nil)]
      (is (= 0 (c {:a 1} {:a 1})))
      (is (> 0 (c {:a 1} {:a 2})))
      (is (< 0 (c {:a 2} {:a 1})))))
  (testing "Ordering by a field."
    (let [c (compare-for [:b :asc])
          cd (compare-for [:b :desc])]
      (testing "Ascent."
        (ordering-by-field c >))
      (testing "Descent."
        (ordering-by-field cd <))))
  (testing "Ordering by multiple fields."
    (let [c (compare-for [:b :asc :c :asc])
          cd (compare-for [:b :desc :c :desc])
          cad (compare-for [:b :asc :c :desc])
          cda (compare-for [:b :desc :c :asc])]
      (ordering-by-2-fields c < <)
      (ordering-by-2-fields cd > >)
      (ordering-by-2-fields cad < >)
      (ordering-by-2-fields cda > <))))

(defn as-longs
  "Converts retrieved records into a numerical form
  by treating each field as a bit in binary.
  s: a sequence of records, each with fields :a :b :c
  return: a long value, built from the binary in the fields"
  [s]
  (map (fn [{:keys [a b c]}] (Long/parseLong (str a b c) 2)) s))

(deftest retrieve-test
  (testing "Retrieving data."
    (let [s' (create-storage "test-append")
          s (append! s' [{:a 0 :b 0 :c 0}    ;; 0
                         {:a 0 :b 0 :c 1}    ;; 1
                         {:a 0 :b 1 :c 0}    ;; 2
                         {:a 0 :b 1 :c 1}    ;; 3
                         {:a 1 :b 0 :c 0}    ;; 4
                         {:a 1 :b 0 :c 1}    ;; 5
                         {:a 1 :b 1 :c 0}    ;; 6
                         {:a 1 :b 1 :c 1}])] ;; 7
      (testing "Unordered."
        (is (= [0 1 2 3 4 5 6 7] (as-longs (retrieve s)))))
      (testing "Ordered by :b"
        (is (= [0 1 4 5 2 3 6 7] (as-longs (retrieve s :b :asc))))
        (is (= [2 3 6 7 0 1 4 5] (as-longs (retrieve s :b :desc)))))
      (testing "Ordered by :b, then :c."
        (is (= [0 4 1 5 2 6 3 7] (as-longs (retrieve s :b :asc :c :asc))))
        (is (= [1 5 0 4 3 7 2 6] (as-longs (retrieve s :b :asc :c :desc))))
        (is (= [2 6 3 7 0 4 1 5] (as-longs (retrieve s :b :desc :c :asc))))
        (is (= [3 7 2 6 1 5 0 4] (as-longs (retrieve s :b :desc :c :desc)))))
      (testing "Ordered by :b, then :c, then :a."
        (is (= [0 4 1 5 2 6 3 7] (as-longs (retrieve s :b :asc :c :asc :a :asc))))
        (is (= [1 5 0 4 3 7 2 6] (as-longs (retrieve s :b :asc :c :desc :a :asc))))
        (is (= [2 6 3 7 0 4 1 5] (as-longs (retrieve s :b :desc :c :asc :a :asc))))
        (is (= [3 7 2 6 1 5 0 4] (as-longs (retrieve s :b :desc :c :desc :a :asc))))
        (is (= [4 0 5 1 6 2 7 3] (as-longs (retrieve s :b :asc :c :asc :a :desc))))
        (is (= [5 1 4 0 7 3 6 2] (as-longs (retrieve s :b :asc :c :desc :a :desc))))
        (is (= [6 2 7 3 4 0 5 1] (as-longs (retrieve s :b :desc :c :asc :a :desc))))
        (is (= [7 3 6 2 5 1 4 0] (as-longs (retrieve s :b :desc :c :desc :a :desc)))))
      (testing "Ordered by an unknown field."
        (is (= [0 1 2 3 4 5 6 7] (as-longs (retrieve s :d :asc))))
        (is (= [0 1 2 3 4 5 6 7] (as-longs (retrieve s :d :desc)))))
      (testing "Ordered by :b, then an unknown field, then :c."
        (is (= [0 4 1 5 2 6 3 7] (as-longs (retrieve s :b :asc :d :asc :c :asc))))
        (is (= [1 5 0 4 3 7 2 6] (as-longs (retrieve s :b :asc :d :asc :c :desc))))
        (is (= [2 6 3 7 0 4 1 5] (as-longs (retrieve s :b :desc :d :asc :c :asc))))
        (is (= [3 7 2 6 1 5 0 4] (as-longs (retrieve s :b :desc :d :asc :c :desc))))
        (is (= [0 4 1 5 2 6 3 7] (as-longs (retrieve s :b :asc :d :desc :c :asc))))
        (is (= [1 5 0 4 3 7 2 6] (as-longs (retrieve s :b :asc :d :desc :c :desc))))
        (is (= [2 6 3 7 0 4 1 5] (as-longs (retrieve s :b :desc :d :desc :c :asc))))
        (is (= [3 7 2 6 1 5 0 4] (as-longs (retrieve s :b :desc :d :desc :c :desc))))))))
