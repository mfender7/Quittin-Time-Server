(ns quittin-time.psql
  (:require [clojure.java.jdbc :as sql]
            [clojure.string :as string]))

(def db {:subprotocol "mysql"
               :subname "//localhost:3306/quittintime"
               :user "root"
               :password ""})

(defn index-of [e coll]
  (first (keep-indexed #(if (= e %2) %1) coll)))

(defn keywordify [items]
  (into {}
        (for [[k v] items]
          [(keyword k) v])))

(defn get-directions [id]
  (sql/query db
             ["select step, direction from recipedirections
              where recipe_id = ? order by step" id]))

(defn get-random-recipes []
  (into [] (sql/query db
             ["call random_recipe;"])))

(defn get-a-recipe [id]
  (let [recipe (sql/query db
             ["select * from recipe where id = ?" id])]))

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
       (map construct-response)))

(defn save-directions [coll id direction]
    (sql/insert! db :recipedirections {:recipe_id id
                                         :step (int (+ (index-of direction coll) 1))
                                         :direction "direction"}))

(defn save-recipe [recipe]
  (sql/insert! db :recipe {:recipe_id (:id recipe)
                             :name (:recipeName recipe)
                             :total_time (:cooktime recipe)
                             :ingredients (clojure.string/join "," (:ingredients recipe))
                             :image_small (:smallUrl (:images recipe))
                             :image_large (:largeUrl (:images recipe))})
  (doseq [d (:directions recipe)]
    (sql/insert! db :recipedirections {:recipe_id (:id recipe)
                                         :step (+ (index-of d (:directions recipe)) 1)
                                         :direction (first d)})))
