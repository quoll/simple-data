(ns ^{:doc "HTTP route handling for the service"
      :author "Paula Gearon"}
    homework.routes
    (:require [clojure.tools.logging :as log]
              [compojure.core :refer [defroutes GET POST]]
              [ring.middleware.defaults :as ring-defaults]
              [ring.util.request :as ring-util]
              [homework.etl :as etl]
              [homework.storage :as storage]
              [homework.http :refer [ok error json json-request? text-post? accept-type]]))

(defn add-data
  "Reads a request and adds the data to the store."
  [{storage :storage :as request}]
  (log/infof "Request to add data %s" (pr-str (dissoc request :storage)))
  (log/debugf "Storage: %s" (:label storage))
  (let [body (ring-util/body-string request)]
    ;; was the post in the correct format
    (if (text-post? request)
      (try
        (log/debugf "Posted text: %s" body)
        ;; determine the correct parser for the body, and then parse the body
        (let [reader (etl/select-reader body)
              data (reader body)]
          ;; only a single item to add, so wrap in a vector
          (storage/append! storage [data])
          (ok))
        ;; log and respond to exceptions
        (catch Exception e
          (log/error (ex-message e))
          (error e)))
      (error (ex-info (str "Bad request data type: " (ring-util/content-type request)) nil)
             (ring-util/body-string request)))))

(defn get-records
  "Retrieve records from storage, and return as a JSON response.
  request: The HTTP request for the records.
  order-by: The ordering to use for the results.
  return: a response structure containing ordered JSON."
  [{storage :storage :as request} order-by]
  (log/infof "Request to retrieve data %s" (pr-str (dissoc request :storage)))
  (log/debugf "Storage: %s" (:label storage))
  ;; check if the request was correct. If not then record this, but continue
  (when-not (json-request? request)
    (log/infof "Non-JSON request made. Request was for: %s" (accept-type request)))
  (json (storage/retrieve storage order-by :asc)))

(defroutes app-routes
  (POST "/records" request (add-data request))
  (GET  "/records/color" request (get-records request :favorite-color))
  (GET  "/records/birthdate" request (get-records request :dob))
  (GET  "/records/name" request (get-records request :last-name)))

(defn wrap-storage
  "Wraps a route to attach storage to requests.
  handler: The routing handler to wrap.
  storage: The storage to attach to the route.
  return: a handler wrapping function."
  [handler storage]
  (log/debugf "wrapping storage %s" (:label storage))
  (fn
    ([request]
     (handler (assoc request :storage storage)))
    ([request response raise]
     (handler (assoc request :storage storage) response raise))))

(defn create-app
  "Creates application routing rules.
  storage: Storage that the application can use.
  return: Application routes with useful services attached."
  [storage]
  (-> app-routes
      ;; most of these wrappers are not needed, but provide for future capabilities
      (ring-defaults/wrap-defaults ring-defaults/api-defaults)
      (wrap-storage storage)))
