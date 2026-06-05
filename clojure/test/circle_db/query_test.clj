(ns circle-db.query-test
  (:require [clojure.test :refer [deftest is]]
            [circle-db.constructs :as c]
            [circle-db.db :as db]
            [circle-db.query :as q]))

(defn- str-attr [value]
  (c/make-attr :name value :db/string))

(defn- db-with-alice []
  (db/add-entity (c/make-db)
                 (assoc (c/make-entity) :attrs {:name (str-attr "Alice")})))

;; --- transform ---

(deftest transform-matches-datom-where-constants-equal
  (let [pred (q/transform ["?e" :name "Alice"])
        datom (c/->Datom 1 :name "Alice")]
    (is (true? (pred datom)))))

(deftest transform-rejects-datom-where-value-differs
  (let [pred (q/transform ["?e" :name "Alice"])
        datom (c/->Datom 2 :name "Bob")]
    (is (false? (pred datom)))))

(deftest transform-variable-slots-are-wildcards
  (let [pred (q/transform ["?e" "?a" "Alice"])]
    (is (true?  (pred (c/->Datom 1  :name "Alice"))))
    (is (true?  (pred (c/->Datom 99 :age  "Alice"))))
    (is (false? (pred (c/->Datom 1  :name "Bob"))))))

;; --- plan ---

(deftest plan-entity-unknown-queries-avet
  (let [d (db-with-alice)
        layer (last (:layers d))
        plan-fn (q/plan [["?e" :name "Alice"]] layer)
        result (plan-fn)]
    (is (contains? result 1))))

(deftest plan-entity-unknown-returns-all-matching-entities
  (let [d (-> (c/make-db)
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")}))
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Bob")}))
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")})))
        layer (last (:layers d))
        plan-fn (q/plan [["?e" :name "Alice"]] layer)
        result (plan-fn)]
    (is (= #{1 3} result))))

(deftest plan-value-unknown-queries-eavt
  (let [d (db-with-alice)
        layer (last (:layers d))
        plan-fn (q/plan [[1 :name "?v"]] layer)
        result (plan-fn)]
    (is (= "Alice" result))))

(deftest plan-attr-unknown-queries-veat
  (let [d (db-with-alice)
        layer (last (:layers d))
        plan-fn (q/plan [[1 "?a" "Alice"]] layer)
        result (plan-fn)]
    (is (contains? result :name))))

(deftest plan-attr-unknown-returns-all-matching-attrs
  (let [d (db/add-entity (c/make-db)
                         (assoc (c/make-entity) :attrs
                                {:name     (str-attr "Alice")
                                 :username (str-attr "Alice")}))
        layer (last (:layers d))
        plan-fn (q/plan [[1 "?a" "Alice"]] layer)
        result (plan-fn)]
    (is (= #{:name :username} result))))

;; --- range scans (2 variables) ---

(defn- db-with-alice-and-bob []
  (-> (c/make-db)
      (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")
                                                     :age  (c/make-attr :age 30 :db/long)}))
      (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Bob")
                                                     :age  (c/make-attr :age 25 :db/long)}))))

(deftest plan-attr-known-scans-avet-range
  (let [d (db-with-alice-and-bob)
        layer (last (:layers d))
        plan-fn (q/plan [["?e" :name "?v"]] layer)
        result (plan-fn)]
    (is (= {"Alice" #{1} "Bob" #{2}} result))))

(deftest plan-entity-known-scans-eavt-range
  (let [d (db-with-alice-and-bob)
        layer (last (:layers d))
        plan-fn (q/plan [[1 "?a" "?v"]] layer)
        result (plan-fn)]
    (is (= {:name "Alice" :age 30} result))))

(deftest plan-value-known-scans-veat-range
  (let [d (db-with-alice-and-bob)
        layer (last (:layers d))
        plan-fn (q/plan [["?e" "?a" "Alice"]] layer)
        result (plan-fn)]
    (is (= {1 #{:name}} result))))
