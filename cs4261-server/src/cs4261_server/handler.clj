(ns cs4261-server.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [clojure.data.json :as json]
            [clojure.contrib.http.agent]))

(defroutes app-routes
  (GET "/recipes" [] (json/write-str { :message "This is some response"}))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
