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
(def yummly-api-key "_app_id=120b31ce&_app_key=da849f95c57c3af4a033cb06a5f0b986")
(def yummly-base-query "&allowedCourse[]=course^course-Main Dishes&maxTotalTimeInSeconds=3600&maxResult=600")
(def yummly-base-food "")
(def yummly-get "http://api.yummly.com/v1/api/recipe/")
(def map-latlon "http://maps.googleapis.com/maps/api/geocode/json?")
(def map-stores "https://maps.googleapis.com/maps/api/place/nearbysearch/json?types=grocery_or_supermarket")
(def map-key "&key=AIzaSyAdALk9fKSutYxvkBmfrODopOu1xuWRddc")

(defn ext-url [url]
  (clojure.string/replace url "/recipe/" "/recipe/external/"))

(defn get-ids [request]
  (json/read-str (get (client/get request ) :body) :key-fn keyword))

(defn get-yummly-body [id]
  (let [resp (client/get (str yummly-get id "?" yummly-api-key))]
    (json/read-str (get resp :body) :key-fn keyword)))

(defn pull-items [data]
  (get (nth (get data :content) 0) :content))

(defn get-actual-url [url]
  (let [res (html/select (html/html-resource (java.net.URL. (ext-url url))) [:td.close])]
    (get (get (nth (get (nth res 0) :content) 1) :attrs) :href)))

(defn get-directions [url]
  (->> (html/select (html/html-resource (java.net.URL. url)) [:div.directions :div.directLeft :li])
       (map pull-items)))

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
  (= (:sourceDisplayName data) "AllRecipes"))

(defn get-random [matches]
  (->> (filter fil? matches)
       (shuffle)
       (map :id)
       (take 3)))

(defn search-recipes [param]
  (let [req (str yummly-search yummly-api-key yummly-base-query "&" param)
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
  (GET "/recipes" [] (json/write-str { :message (fil? {:imageUrlsBySize {:90 "http://lh6.ggpht.com/7mRXudP6Wsr3PKoz0EyAknJ3IKuigaiVdiH4yTdRX8d4AWIStrOXvRSu9MjVmeOO_TZpQI8_WZdzyW9WuTFy_g=s90-c"} :sourceDisplayName "AllRecipes" :ingredients ["boneless skinless chicken breasts","chopped onion","garlic","cream cheese","butter","dough"] :id "Chicken-Puffs-Allrecipes", :smallImageUrls ["http://lh4.ggpht.com/AmQ0hUf_he7dabIq3qZd901wfjrIw4jlPlOPFo3V7IhCS63kyP32WDipGNKWuDWUqcT2EarxuAI0Vs3yikagOA=s90"] :recipeName "Chicken Puffs" :totalTimeInSeconds 2100 :attributes { :course ["Main Dishes","Appetizers","Lunch and Snacks"]} :flavors {:piquant 0.0 :meaty 0.5 :sour 0.8333333333333334,"bitter":0.6666666666666666 :salty 0.5 :sweet 0.3333333333333333} :rating 5})}))
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
