(ns quittin-time.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [clojure.data.json :as json]
            [ring.util.response :as resp]
            [org.httpkit.client :as http]
            [clj-http.client :as client]
            [org.httpkit.server :as hs]
            [quittin-time.psql :as psql]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.jsonp :refer [wrap-json-with-padding]]
            [net.cgrand.enlive-html :as html]))

(def map-latlon "http://maps.googleapis.com/maps/api/geocode/json?")
(def map-stores "https://maps.googleapis.com/maps/api/place/nearbysearch/json?types=grocery_or_supermarket")
(def map-key "&key=AIzaSyAdALk9fKSutYxvkBmfrODopOu1xuWRddc")

(defn get-latlong [param]
  (client/get (str map-latlon param)))

(defn get-stores [param]
  (client/get (str map-stores map-key "&" param)))

(defroutes app-routes
  (GET "/" [] (json/write-str "This is just some start screen to appease myself."))
  (GET "/somerecipe" {params :query-string} (json/write-str "THE HELL WITH IT"))
  (GET "/recipes" [] (json/write-str (psql/get-random)))
  (POST "/user" {params :query-params} (str "todo: update users"))
  (PUT "/user" {params :query-params} (str "todo: create users"))
  (GET "/latlong" {params :query-string} (get-latlong params))
  (GET "/stores" {params :query-string} (get-stores params))
  (route/resources "/")
  (route/not-found "This isn't the page you're looking for.."))

(def app
  (-> #'app-routes
      (wrap-json-with-padding)
      (handler/api)
      (wrap-json-params)))
