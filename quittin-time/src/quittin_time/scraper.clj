(ns quittin-time.scraper
  (:require [clojure.data.json :as json]
            [ring.util.response :as resp]
            [org.httpkit.client :as http]
            [clj-http.client :as client]
            [org.httpkit.server :as hs]
            [net.cgrand.enlive-html :as html]))

(def yummly-search "http://api.yummly.com/v1/api/recipes?")
(def yummly-api-key "_app_id=120b31ce&_app_key=da849f95c57c3af4a033cb06a5f0b986")
(def yummly-base-query "&allowedCourse[]=course^course-Main Dishes&maxTotalTimeInSeconds=3600&maxResult=300")
(def yummly-base-food "")

(defn ext-url [url]
  (clojure.string/replace url "/recipe/" "/recipe/external/"))

(defn get-json [request]
  (json/read-str (get (client/get request ) :body) :key-fn keyword))

(defn pull-items [data]
  (get (nth (get data :content) 0) :content))

(defn get-actual-url [url]
  (let [res (html/select (html/html-resource (java.net.URL. (ext-url url))) [:td.close])]
    (get (get (nth (get (nth res 0) :content) 1) :attrs) :href)))

(defn all-recipes-directions [url]
  (->> (html/select (html/html-resource (java.net.URL. url)) [:div.directions :div.directLeft :li])
       (map pull-items)))

(defn kitchn-directions []
  )

(defn get-directions [match]
  (condp = (:sourceDisplayName match)
    "AllRecipes" (all-recipes-directions match)
    "The Kitchn" (kitchn-directions match)))

(defn call-api []
  (:matches (get-json (str yummly-search yummly-api-key yummly-base-query))))

(defn fil? [data]
  (or (= (:sourceDisplayName data) "AllRecipes")
      (= (:sourceDisplayName data) "The Kitchn")))

(defn get-recipes []
  (->> (call-api)
       (filter fil?)
       (get-directions)))

(defn testa []
  (str "calling from another file/class/whatever"))
