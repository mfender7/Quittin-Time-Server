(ns quittin-time.scraper
  (:require [clojure.data.json :as json]
            [ring.util.response :as resp]
            [org.httpkit.client :as http]
            [clj-http.client :as client]
            [org.httpkit.server :as hs]
            [quittin-time.psql :as psql]
            [net.cgrand.enlive-html :as html]))

(def yummly-search "http://api.yummly.com/v1/api/recipes?")
(def yummly-api-key "_app_id=120b31ce&_app_key=da849f95c57c3af4a033cb06a5f0b986")
(def yummly-base-query "&allowedCourse[]=course^course-Main Dishes&maxTotalTimeInSeconds=3600&maxResult=600")
(def yummly-get "http://api.yummly.com/v1/api/recipe/")

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
       (map :id)))

(defn search-recipes []
  (let [req (str yummly-search yummly-api-key yummly-base-query)
        matches (:matches (get-ids req))]
    (->> (get-random matches)
         (map get-recipe)
         (map psql/save-recipe))
    (str "Success!")))
