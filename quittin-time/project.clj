(defproject quittin-time "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "www.fenderco.de"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.6"]
                 [org.clojure/data.json "0.2.5"]
                 [http-kit "2.1.6"]
                 [clj-http "0.9.2"]
                 [ring/ring-core "1.2.0"]
                 [ring/ring-core "1.2.0"]
                 [ring/ring-json "0.3.1"]
                 [enlive "1.1.5"]
                 [ring.middleware.jsonp "0.1.4"]
                 [korma "0.3.0"]
                 [org.postgresql/postgresql "9.3-1102-jdbc4"]]
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler quittin-time.handler/app})
