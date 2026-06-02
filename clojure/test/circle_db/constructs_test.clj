(ns circle-db.constructs-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [circle-db.constructs :as c]))

(deftest datom-holds-three-fields
  (let [d (c/->Datom 1 :name "Alice")]
    (is (= 1 (:entity-id d)))
    (is (= :name (:attr-name d)))
    (is (= "Alice" (:value d)))))

(deftest attribute-fields-and-defaults
  (let [a (c/make-attr :name "Alice" :db/string)]
    (is (= :name (:name a)))
    (is (= "Alice" (:value a)))
    (is (= :db/string (:type a)))
    (is (= :db/single (:cardinality a)))
    (is (= -1 (:ts a)))
    (is (= -1 (:prev-ts a)))))

(deftest attribute-multiple-cardinality
  (let [a (c/make-attr :tags #{} :db/string :cardinality :db/multiple)]
    (is (= :db/multiple (:cardinality a)))))

(deftest entity-has-id-and-empty-attrs
  (let [e (c/make-entity 1)]
    (is (= 1 (:id e)))
    (is (= {} (:attrs e)))))

(deftest layer-has-storage-and-four-empty-indexes
  (let [layer (c/make-layer)]
    (is (= {} (:storage layer)))
    (is (= {} (:eavt layer)))
    (is (= {} (:avet layer)))
    (is (= {} (:veat layer)))
    (is (= {} (:vaet layer)))))

(deftest make-db-returns-empty-database
  (let [db (c/make-db)]
    (is (= 1 (count (:layers db))))
    (is (instance? circle_db.constructs.Layer (first (:layers db))))
    (is (= 0 (:top-id db)))
    (is (= 0 (:curr-time db)))))

(deftest database-nests-layers
  (let [db (c/make-db)]
    (is (= {} (:eavt (first (:layers db)))))))
