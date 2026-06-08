(ns circle-db.time-travel-test
  (:require [clojure.test :refer [deftest is]]
            [circle-db.constructs :as c]
            [circle-db.db :as db]
            [circle-db.query :as q]
            [circle-db.time-travel :as tt]))

(defn- str-attr [value]
  (c/make-attr :name value :db/string))

;; --- advance-time ---

(deftest advance-time-increments-curr-time
  (let [d (c/make-db)]
    (is (= (inc (:curr-time d)) (:curr-time (tt/advance-time d))))))

;; --- evolution-of ---

(deftest evolution-of-single-update-returns-one-entry
  (let [d (-> (c/make-db)
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")}))
              (tt/advance-time)
              (db/update-entity 1 :name "Bob"))
        result (tt/evolution-of d 1 :name)]
    (is (= [[1 "Bob"]] (vec result)))))

(deftest evolution-of-three-updates-returns-all-in-order
  (let [d (-> (c/make-db)
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")}))
              (tt/advance-time)
              (db/update-entity 1 :name "Bob")
              (tt/advance-time)
              (db/update-entity 1 :name "Charlie")
              (tt/advance-time)
              (db/update-entity 1 :name "Dave"))
        result (tt/evolution-of d 1 :name)]
    (is (= [[1 "Bob"] [2 "Charlie"] [3 "Dave"]] (vec result)))))

(deftest evolution-of-no-updates-returns-nil-or-empty
  (let [d (-> (c/make-db)
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")})))
        result (tt/evolution-of d 1 :name)]
    (is (empty? result))))

;; --- db-at ---

(deftest db-at-query-reflects-state-at-timestamp
  (let [d (-> (c/make-db)
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")}))
              (tt/advance-time)
              (db/update-entity 1 :name "Bob")
              (tt/advance-time)
              (db/update-entity 1 :name "Charlie"))
        at-1  (tt/db-at d 1)
        at-now d]
    (is (= [[1 "Bob"]]     (vec (q/q {:find ["?e" "?v"] :where [["?e" :name "?v"]]} at-1))))
    (is (= [[1 "Charlie"]] (vec (q/q {:find ["?e" "?v"] :where [["?e" :name "?v"]]} at-now))))))
