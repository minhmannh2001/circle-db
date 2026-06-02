(ns circle-db.storage-test
  (:require [clojure.test :refer [deftest is]]
            [circle-db.constructs :as c]
            [circle-db.storage :as s]))

(deftest add-entity-then-get-it-back
  (let [e (c/make-entity 1)
        storage (s/add-entity {} e)]
    (is (= e (s/get-entity storage 1)))))

(deftest add-entity-does-not-mutate-original
  (let [original {}]
    (s/add-entity original (c/make-entity 1))
    (is (= {} original))))

(deftest get-entity-returns-nil-for-missing-id
  (is (nil? (s/get-entity {} 99))))

(deftest remove-entity-removes-correct-entity
  (let [e1 (c/make-entity 1)
        e2 (c/make-entity 2)
        storage (-> {} (s/add-entity e1) (s/add-entity e2))
        result (s/remove-entity storage 1)]
    (is (nil? (s/get-entity result 1)))
    (is (= e2 (s/get-entity result 2)))))

(deftest remove-entity-does-not-mutate-original
  (let [e (c/make-entity 1)
        original (s/add-entity {} e)]
    (s/remove-entity original 1)
    (is (= e (s/get-entity original 1)))))
