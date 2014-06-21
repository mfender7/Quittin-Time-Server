(ns cs4261-server.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [clojure.data.json :as json]
            [ring.util.response :as resp]
            [org.httpkit.client :as http]
            [org.httpkit.server :as hs]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.jsonp :refer [wrap-json-with-padding]]))

(defn get-recipes [param]
  (let [req (str "http://api.yummly.com/v1/api/recipes?_app_id=78dccce1&_app_key=87460fca330c28f52a9603ababd5a54f&allowedCourse[]=course^course-Dinner&maxTotalTimeInSeconds=3600&" param)]
    (str req)))


(defroutes app-routes
  (GET "/somerecipe" {params :query-string} (get-recipes params))
  (GET "/recipes" [] (json/write-str { :message "This is some other different response"}))
  (route/resources "/")
  (route/not-found "This isn't the page you're looking for.."))

(def app
  (-> #'app-routes
      (handler/api)
      (wrap-json-params)))


;http://api.yummly.com/v1/api/recipes?_app_id=78dccce1&_app_key=87460fca330c28f52a9603ababd5a54f&allowedCourse[]=course^course-Dinner&maxTotalTimeInSeconds=3600
;your _search_parameters
