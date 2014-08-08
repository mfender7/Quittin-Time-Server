(ns quittin-time.psql
  (:require [clojure.java.jdbc :as sql]
            [clojure.string :as string]))

(def conn "postgresql://80.74.134.201:5432/quittintime")

(defn strip-id [item]
  (apply dissoc item [:recipe_id]))


(defn keywordify [items]
  (into {}
        (for [[k v] items]
          [(keyword k) v])))

(defn get-directions [id]
  (let [dir (sql/query conn
             ["select hstore(r) from recipedirections as r
              where r.recipe_id = ? order by step" id])]
    (->> (map :hstore dir)
         (map keywordify))))


(defn get-random-recipes []
  (into [] (map :hstore (sql/query conn
             ["select hstore(r) from recipe as r order by random() limit 3"]))))

(defn get-a-recipe [id]
  (let [recipe (sql/query conn
             ["select hstore(r) from recipe as r where r.id = ?" id])]))

(defn construct-response [item]
  (apply array-map [:id (:id item)
                    :recipe_id (:recipe_id item)
                    :name (:name item)
                    :total_time (:total_time item)
                    :ingredients (clojure.string/split (:ingredients item) #",")
                    :image_small (:image_small item)
                    :image_large (:image_large item)
                    :directions (get-directions (:recipe_id item))]))

(defn get-random []
  (->> (get-random-recipes)
       (map keywordify)
       (map construct-response))
  )
