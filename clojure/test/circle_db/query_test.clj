(ns circle-db.query-test
  (:require [clojure.test :refer [deftest is]]
            [circle-db.constructs :as c]
            [circle-db.db :as db]
            [circle-db.query :as q]
            [clojure.set :as set]))

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

;; --- execute ---

(deftest execute-entity-unknown-returns-bindings
  (let [d (db-with-alice)
        layer (last (:layers d))
        result (q/execute [["?e" :name "Alice"]] layer)]
    (is (= [{"?e" 1}] (vec result)))))

(deftest execute-value-variable-bound-in-range-scan
  (let [d (-> (c/make-db)
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")}))
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Bob")})))
        layer (last (:layers d))
        result (set (q/execute [["?e" :name "?name"]] layer))]
    (is (contains? result {"?e" 1 "?name" "Alice"}))
    (is (contains? result {"?e" 2 "?name" "Bob"}))
    (is (= 2 (count result)))))

(deftest execute-two-clause-and-intersects-entity-sets
  (let [d (-> (c/make-db)
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")
                                                             :age  (c/make-attr :age 30 :db/long)}))
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Bob")
                                                             :age  (c/make-attr :age 25 :db/long)}))
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")
                                                             :age  (c/make-attr :age 25 :db/long)})))
        layer (last (:layers d))
        result (q/execute [["?e" :name "Alice"] ["?e" :age 30]] layer)]
    (is (= [{"?e" 1}] (vec result)))))

;; --- unify ---

(deftest unify-projects-single-find-var
  (let [bindings [{"?e" 1 "?name" "Alice"} {"?e" 2 "?name" "Bob"}]
        result (set (q/unify bindings ["?e"]))]
    (is (= #{[1] [2]} result))))

(deftest unify-projects-multiple-find-vars-in-order
  (let [bindings [{"?e" 1 "?name" "Alice"} {"?e" 2 "?name" "Bob"}]
        result (set (q/unify bindings ["?name" "?e"]))]
    (is (= #{["Alice" 1] ["Bob" 2]} result))))

;; --- q ---

(deftest q-single-clause-returns-matching-entities
  (let [d (-> (c/make-db)
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")}))
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Bob")})))
        result (q/q {:find ["?e"] :where [["?e" :name "Alice"]]} d)]
    (is (= [[1]] (vec result)))))

(deftest q-two-clause-where-returns-only-entities-matching-all-clauses
  (let [d (-> (c/make-db)
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")
                                                             :age  (c/make-attr :age 30 :db/long)}))
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Bob")
                                                             :age  (c/make-attr :age 25 :db/long)}))
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")
                                                             :age  (c/make-attr :age 25 :db/long)})))
        result (q/q {:find ["?e"] :where [["?e" :name "Alice"] ["?e" :age 30]]} d)]
    (is (= [[1]] (vec result)))))

(deftest q-historical-layer-returns-old-state
  (let [d-before (-> (c/make-db)
                     (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")})))
        d-after  (db/add-entity d-before (assoc (c/make-entity) :attrs {:name (str-attr "Bob")}))
        current  (set (q/q {:find ["?e" "?name"] :where [["?e" :name "?name"]]} d-after))
        historic (vec (q/q {:find ["?e" "?name"] :where [["?e" :name "?name"]]} d-before))]
    (is (= 2 (count current)))
    (is (= [[1 "Alice"]] historic))))

;; --- attr-unknown queries ---

(deftest q-attr-unknown-returns-entity-and-attr
  ;; In Clojure, attr names in indexes are keywords, so result is [1 :name]
  (let [d (-> (c/make-db)
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")
                                                             :age  (c/make-attr :age 30 :db/long)}))
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Bob")})))
        result (q/q {:find ["?e" "?a"] :where [["?e" "?a" "Alice"]]} d)]
    (is (= [[1 :name]] (vec result)))))

(deftest q-attr-unknown-expands-multiple-attrs-per-entity
  ;; Entity 1 has both :name and :nickname = "Alice" → 2 rows
  (let [d (-> (c/make-db)
              (db/add-entity (assoc (c/make-entity) :attrs {:name     (str-attr "Alice")
                                                             :nickname (str-attr "Alice")}))
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Bob")})))
        result (set (q/q {:find ["?e" "?a"] :where [["?e" "?a" "Alice"]]} d))]
    (is (= #{[1 :name] [1 :nickname]} result))))

(deftest q-attr-unknown-combined-with-other-clause
  ;; Only entity 1 (Alice, age=30) satisfies both clauses
  (let [d (-> (c/make-db)
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")
                                                             :age  (c/make-attr :age 30 :db/long)}))
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Bob")
                                                             :age  (c/make-attr :age 25 :db/long)}))
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")
                                                             :age  (c/make-attr :age 25 :db/long)})))
        result (q/q {:find ["?e" "?a"] :where [["?e" "?a" "Alice"] ["?e" :age 30]]} d)]
    (is (= [[1 :name]] (vec result)))))
