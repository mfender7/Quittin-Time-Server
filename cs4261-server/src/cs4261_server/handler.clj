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
            [ring.middleware.jsonp :refer [wrap-json-with-padding]]))

(def base-get "http://api.yummly.com/v1/api/recipe/")
(def base-search "http://api.yummly.com/v1/api/recipes?")
(def api-key "_app_id=78dccce1&_app_key=87460fca330c28f52a9603ababd5a54f")
(def base-query "&allowedCourse[]=course^course-Dinner&maxTotalTimeInSeconds=3600&")

(defn get-ids [request]
  (json/read-str (get (client/get request) :body) :key-fn keyword))

(defn get-recipe [id]
  (let [resp (client/get (str base-get id "?" api-key))]
    (let [body (json/read-str (get resp :body) :key-fn keyword)]
      (json/write-str {:title id, :ingredients (:ingredientLines body)}))))

(defn get-random [matches]
  (->> (shuffle matches)
       (map :id)
       (take 3)))

(defn search-recipes [param]
  (let [req (str base-search api-key base-query param)
        matches (:matches (get-ids req))]
    (->> (get-random matches)
         (map get-recipe))))

(defroutes app-routes
  (GET "/" [] (json/write-str "This is just some start screen to appease myself."))
  (GET "/somerecipe" {params :query-string} (search-recipes params))
  (GET "/recipes" [] (json/write-str { :message "This is some other different response"}))
  (route/resources "/")
  (route/not-found "This isn't the page you're looking for.."))

(def app
  (-> #'app-routes
      (wrap-json-with-padding)
      (handler/api)
      (wrap-json-params)))
