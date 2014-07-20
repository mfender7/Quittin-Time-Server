(ns cs4261-server.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [clojure.data.json :as json]
            [ring.util.response :as resp]
            [org.httpkit.client :as http]
            [clj-http.client :as client]
            [org.httpkit.server :as hs]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.jsonp :refer [wrap-json-with-padding]]
            [net.cgrand.enlive-html :as html]))

(def yummly-search "http://api.yummly.com/v1/api/recipes?")
(def yummly-api-key "_app_id=78dccce1&_app_key=87460fca330c28f52a9603ababd5a54f")
(def yummly-base-query "&allowedCourse[]=course^course-Main Dishes&maxTotalTimeInSeconds=3600")
(def yummly-base-food "")
(def yummly-get "http://api.yummly.com/v1/api/recipe/")
(def map-latlon "http://maps.googleapis.com/maps/api/geocode/json?")
(def map-stores "https://maps.googleapis.com/maps/api/place/nearbysearch/json?types=grocery_or_supermarket")
(def map-key "&key=AIzaSyAdALk9fKSutYxvkBmfrODopOu1xuWRddc")

(defn ext-url [url]
  (clojure.string/replace url "/recipe/" "/recipe/external/"))

(defn get-ids [request]
  (json/read-str (get (client/get request) :body) :key-fn keyword))

(defn get-yummly-body [id]
  (let [resp (client/get (str yummly-get id "?" yummly-api-key))]
    (json/read-str (get resp :body) :key-fn keyword)))

(defn criteria? [data]
  (and (> (.length (str (get (get data :attrs) :class))) 0)
       (.contains (get (get data :attrs) :class) "item")
       (or (.contains (get (get data :attrs) :class) "instruction ")
           (.contains (get (get data :attrs) :class) "direction "))))

(defn filter-data? [data]
  (not (= data "")))

(defn pull-items [data]
  (if (criteria? data)
    (get (nth (get data :content) 3) :content)
    (str "")))

(defn get-html-resource [url]
  (->> (html/select (html/html-resource (java.net.URL. url)) [:li])
       (map pull-items)
       (filter filter-data?)))

(defn get-actual-url [url]
  (let [res (html/select (html/html-resource (java.net.URL. (ext-url url))) [:td.close])]
    (get (get (nth (get (nth res 0) :content) 1) :attrs) :href)))

(defn get-directions [url]
  (->> (html/select (html/html-resource (java.net.URL. url)) [:li])
       (map pull-items)
       (filter filter-data?)))

(defn get-recipe [id]
  (let [resp (client/get (str yummly-get id "?" yummly-api-key))
        body (json/read-str (get resp :body) :key-fn keyword)]
      (apply array-map [:title id
      				   :ingredients (:ingredientLines body)
                 :recipeName (:name body)
      				   :directions (get-directions (get-actual-url (:url (:attribution body))))
      				   :images (array-map :smallUrl ((nth (:images body) 0) :hostedSmallUrl) :largeUrl ((nth (:images body) 0) :hostedLargeUrl))
      				   :cooktime (:totalTime body)
      				   :id (:id body)])))

(defn fil? [data]
  (= (:sourceDisplayName data) "Martha Stewart"))

(defn get-random [matches]
  (->> (shuffle matches)
       (filter fil?)
       (map :id)
       (take 3)))

(defn search-recipes [param]
  (let [req (str yummly-search yummly-api-key yummly-base-query yummly-base-food)
        matches (:matches (get-ids req))]
    (->> (get-random matches)
         (map get-recipe))))

(defn get-latlong [param]
  (client/get (str map-latlon param)))

(defn get-stores [param]
  (client/get (str map-stores map-key "&" param)))

(defroutes app-routes
  (GET "/" [] (json/write-str "This is just some start screen to appease myself."))
  (GET "/somerecipe" {params :query-string} (json/write-str {:matches (search-recipes params)}))
  (GET "/recipes" [] (json/write-str { :message (get-directions "http://www.marthastewart.com/326813/coconut-bars")}))
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
