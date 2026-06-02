(ns circle-db.eavt-test
  (:require [clojure.test :refer [deftest is]]
            [circle-db.constructs :as c]
            [circle-db.eavt :as e]
            [circle-db.layer :as l]))

(deftest add-datom-then-get-entity-attributes
  (let [d (c/->Datom 1 :name "Alice")
        index (e/index-add {} d)]
    (is (= {:name "Alice"} (e/index-get index 1)))))

(deftest two-datoms-same-entity-both-returned
  (let [d1 (c/->Datom 1 :name "Alice")
        d2 (c/->Datom 1 :age 30)
        index (-> {} (e/index-add d1) (e/index-add d2))]
    (is (= {:name "Alice" :age 30} (e/index-get index 1)))))

(deftest index-get-specific-attr
  (let [d1 (c/->Datom 1 :name "Alice")
        d2 (c/->Datom 1 :age 30)
        index (-> {} (e/index-add d1) (e/index-add d2))]
    (is (= "Alice" (e/index-get index 1 :name)))
    (is (= 30 (e/index-get index 1 :age)))))

(deftest remove-datom-leaves-other-attrs
  (let [d1 (c/->Datom 1 :name "Alice")
        d2 (c/->Datom 1 :age 30)
        index (-> {} (e/index-add d1) (e/index-add d2))
        result (e/index-remove index d1)]
    (is (nil? (e/index-get result 1 :name)))
    (is (= 30 (e/index-get result 1 :age)))))

(deftest index-add-does-not-mutate-original
  (let [original {}]
    (e/index-add original (c/->Datom 1 :name "Alice"))
    (is (= {} original))))

(deftest add-entity-to-layer-updates-storage-and-eavt
  (let [attr (c/make-attr :name "Alice" :db/string)
        entity (c/->Entity 1 {:name attr})
        layer (c/make-layer)
        new-layer (l/add-entity-to-layer layer entity)]
    (is (= entity (get (:storage new-layer) 1)))
    (is (= "Alice" (e/index-get (:eavt new-layer) 1 :name)))))
