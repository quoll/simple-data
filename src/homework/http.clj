(ns ^{:doc "HTTP request processing and responses"
      :author "Paula Gearon"}
    homework.http
    (:require [clojure.string :as str]
              [clojure.data.json :as json]
              [ring.util.request :as ring-util]
              [homework.etl :as etl]))

(def plain-headers {"Content-Type" "text/plain"})
(def json-headers {"Content-Type" "application/json; charset=utf-8"})

(defn date-writer
  "Rewrites the :dob field from an object to a string for JSON conversion.
  k: The key to check for the :dob field.
  v: The value for the provided key.
  return: A string form of the value if the k is :dob, or the original value if it is not."
  [k v]
  (if (= :dob k) (etl/date-str v) v))

(defn ok
  "Returns an OK response.
  body: optional data to return."
  ([] {:headers plain-headers
       :status 200})
  ([body] {:headers plain-headers
           :status 200
           :body body}))

(defn json
  "Returns a JSON response.
  body: data to return"
  [body]
  {:headers json-headers
   :status 200
   :body (json/write-str body :value-fn date-writer)})

(defn error
  "Returns an error response.
  e: The exception that caused the error.
  body: An optional body to include with the error."
  ([e] {:status 400
        :headers plain-headers
        :body (ex-message e)})
  ([e body]
   (assoc (error e) :body body)))

(defn accept-type
  "Returns the content type for a GET request.
  request: the request to check.
  return: the content type in the request."
  [request]
  (get-in request [:headers "accept"]))

(defn json-request?
  "Determines if a request was for JSON data.
  request: the request to check.
  return: true if the Accept header requests JSON."
  [request]
  (some-> (accept-type request)
          (str/starts-with? "application/json")))

(defn text-post?
  "Determines if a request was made by posting text.
  request: the request to check.
  return: true if the Content-Type header is for text."
  [request]
  (some-> (ring-util/content-type request)
          (str/starts-with? "text/plain")))
