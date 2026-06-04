(ns circle-db.transactions-test
  (:require [clojure.test :refer [deftest is]]
            [circle-db.constructs :as c]
            [circle-db.db :as db]
            [circle-db.transactions :as tx]))

(defn- str-attr [value]
  (c/make-attr :name value :db/string))

(defn- add-alice [d]
  (db/add-entity d (assoc (c/make-entity) :attrs {:name (str-attr "Alice")})))

(defn- add-bob [d]
  (db/add-entity d (assoc (c/make-entity) :attrs {:name (str-attr "Bob")})))

;; --- transact ---

(deftest transact-two-ops-adds-exactly-one-layer
  (let [db-atom (atom (c/make-db))
        before-count (count (:layers @db-atom))
        _ (tx/transact db-atom [add-alice add-bob])]
    (is (= (inc before-count) (count (:layers @db-atom))))))

(deftest transact-all-entities-visible-in-result
  (let [db-atom (atom (c/make-db))
        _ (tx/transact db-atom [add-alice add-bob])
        layer (last (:layers @db-atom))]
    (is (= "Alice" (get-in layer [:storage 1 :attrs :name :value])))
    (is (= "Bob"   (get-in layer [:storage 2 :attrs :name :value])))))

(deftest transact-increments-curr-time
  (let [db-atom (atom (c/make-db))
        before-time (:curr-time @db-atom)
        _ (tx/transact db-atom [add-alice])]
    (is (= (inc before-time) (:curr-time @db-atom)))))

;; --- what-if ---

(deftest what-if-result-has-changes
  (let [db (c/make-db)
        result (tx/what-if db [add-alice add-bob])
        layer (last (:layers result))]
    (is (= "Alice" (get-in layer [:storage 1 :attrs :name :value])))
    (is (= "Bob"   (get-in layer [:storage 2 :attrs :name :value])))))

(deftest what-if-original-db-unchanged
  (let [db (c/make-db)
        original-count (count (:layers db))
        _ (tx/what-if db [add-alice add-bob])]
    (is (= original-count (count (:layers db))))))

(deftest transact-historical-layer-has-pre-transaction-state
  (let [db-atom (atom (c/make-db))
        _ (tx/transact db-atom [add-alice])
        _ (tx/transact db-atom [add-bob])
        pre-last (nth (:layers @db-atom) (- (count (:layers @db-atom)) 2))]
    (is (contains? (:storage pre-last) 1))
    (is (not (contains? (:storage pre-last) 2)))))
