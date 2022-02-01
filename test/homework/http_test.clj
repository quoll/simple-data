(ns homework.http-test
  (:require [clojure.test :refer [is deftest testing]]
            [homework.http :refer [date-writer ok json error accept-type json-request? text-post?]])
  (:import [java.time LocalDate]))

(def plain-text {"Content-Type" "text/plain"})

(deftest date-writer-test
  (testing "Date values are converted when the key is DOB"
    (is (= "12/25/2021" (date-writer :dob (LocalDate/of 2021 12 25))))
    (is (= "7/4/2021" (date-writer :dob (LocalDate/of 2021 7 4)))))
  (testing "Date values are not when the key is anything else"
    (is (instance? LocalDate (date-writer :date (LocalDate/of 2021 12 25))))
    (is (= "Austin" (date-writer :last-name "Austin"))))
  (testing "Attempts to convert invalid dob lead to exceptions"
    (is (thrown? Exception (date-writer :dob "12/25/2021")))))

(deftest ok-test
  (testing "Basic structure."
    (is (= {:headers plain-text :status 200} (ok))))
    (is (= {:headers plain-text :status 200 :body "data"} (ok "data"))))

(deftest json-test
  (testing "Basic structure."
    (is (= {:headers {"Content-Type" "application/json; charset=utf-8"} :status 200
            :body "{\"key\":\"value\",\"number\":5}"}
           (json {:key "value" :number 5}))))
  (testing "Time values."
    (is (= {:headers {"Content-Type" "application/json; charset=utf-8"} :status 200
            :body "{\"key\":\"value\",\"number\":5,\"dob\":\"1\\/28\\/1813\"}"}
           (json {:key "value" :number 5 :dob (LocalDate/of 1813 1 28)})))))

(deftest error-test
  (testing "Basic structure"
    (is (= {:status 400 :headers plain-text :body "a message"}
           (error (ex-info "a message" {}))))
    (is (= {:status 400 :headers plain-text :body "message body"}
           (error (ex-info "a message" {}) "message body")))))

;; samples from CURL
;; curl -H "Accept: application/json" http://localhost:8080/records/name
(def sample-json-get
  {:ssl-client-cert nil, :protocol "HTTP/1.1", :remote-addr "[0:0:0:0:0:0:0:1]", :params {}, :route-params {}, 
   :headers {"accept" "application/json", "user-agent" "curl/7.77.0", "host" "localhost:8080"}, 
   :server-port 8080, :content-length nil, :form-params {}, :compojure/route [:get "/records/name"], 
   :query-params {}, :content-type nil, :character-encoding nil, :uri "/records/name", 
   :server-name "localhost", :query-string nil, :body "", :scheme :http, :request-method :get})

;; curl -H "Accept: text/plain" http://localhost:8080/records/name
(def sample-text-get
  {:ssl-client-cert nil, :protocol "HTTP/1.1", :remote-addr "[0:0:0:0:0:0:0:1]", :params {}, :route-params {}, 
   :headers {"accept" "text/plain", "user-agent" "curl/7.77.0", "host" "localhost:8080"}, 
   :server-port 8080, :content-length nil, :form-params {}, :compojure/route [:get "/records/name"], 
   :query-params {}, :content-type nil, :character-encoding nil, :uri "/records/name", 
   :server-name "localhost", :query-string nil, :body "", :scheme :http, :request-method :get})

;; curl -X POST -H "Content-type: text/plain" http://localhost:8080/records -d "Bingley|Charles|chuck@netherfield.co.uk|blue|07/10/1807"
(def sample-post
  {:ssl-client-cert nil, :protocol "HTTP/1.1", :remote-addr "[0:0:0:0:0:0:0:1]", :params {}, :route-params {}, 
   :headers {"accept" "*/*", "user-agent" "curl/7.77.0", "host" "localhost:8080", 
             "content-length" "55", "content-type" "text/plain"}, 
   :server-port 8080, :content-length 55, :form-params {}, :compojure/route [:post "/records"],
   :query-params {}, :content-type "text/plain", :character-encoding nil, :uri "/records", 
   :server-name "localhost", :query-string nil,
   :body "Bingley|Charles|chuck@netherfield.co.uk|blue|07/10/1807", :scheme :http, :request-method :post})

;; curl -v -X POST -H "Content-type: application/json" http://localhost:8080/records -d "Bingley|Charles|chuck@netherfield.co.uk|blue|07/10/1807"
(def sample-json-post
  {:ssl-client-cert nil, :protocol "HTTP/1.1", :remote-addr "[0:0:0:0:0:0:0:1]", :params {}, :route-params {}, 
   :headers {"accept" "*/*", "user-agent" "curl/7.77.0", "host" "localhost:8080", 
             "content-length" "55", "content-type" "application/json"}, 
   :server-port 8080, :content-length 55, :form-params {}, :compojure/route [:post "/records"], 
   :query-params {}, :content-type "application/json", :character-encoding "UTF-8", :uri "/records", 
   :server-name "localhost", :query-string nil,
   :body "Bingley|Charles|chuck@netherfield.co.uk|blue|07/10/1807", 
   :scheme :http, :request-method :post})

(deftest accept-type-test
  (testing "Basic structure test"
    (is (= "application/json" (accept-type sample-json-get)))))

(deftest json-request-test
  (testing "Basic structure test"
    (is (json-request? sample-json-get))
    (is (not (json-request? sample-text-get))))
  (testing "Missing headers do not lead to NPE."
    (is (nil? (json-request? (dissoc sample-json-get :headers))))))

(deftest text-post-test
  (testing "Basic structure test"
    (is (text-post? sample-post))
    (is (not (text-post? sample-json-post))))
  (testing "Missing headers do not lead to NPE."
    (is (nil? (text-post? (dissoc sample-post :headers))))))
