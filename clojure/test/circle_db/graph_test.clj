(ns circle-db.graph-test
  (:require [clojure.test :refer [deftest is]]
            [circle-db.constructs :as c]
            [circle-db.db :as db]
            [circle-db.graph :as g]))

(defn- ref-attr [target-id]
  (c/make-attr :friend target-id :db/ref))

(defn- str-attr [value]
  (c/make-attr :name value :db/string))

;; --- outgoing-refs ---

(deftest outgoing-refs-returns-referenced-entity-ids
  (let [d (-> (c/make-db)
              (db/add-entity (assoc (c/make-entity) :attrs {}))
              (db/add-entity (assoc (c/make-entity) :attrs {:friend (ref-attr 1)})))
        result (g/outgoing-refs d 2)]
    (is (= [1] (vec result)))))

(deftest outgoing-refs-empty-when-no-refs
  (let [d (-> (c/make-db)
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")})))]
    (is (empty? (g/outgoing-refs d 1)))))

;; --- incoming-refs ---

(deftest incoming-refs-returns-referencing-entity-ids
  (let [d (-> (c/make-db)
              (db/add-entity (assoc (c/make-entity) :attrs {}))
              (db/add-entity (assoc (c/make-entity) :attrs {:friend (ref-attr 1)}))
              (db/add-entity (assoc (c/make-entity) :attrs {:friend (ref-attr 1)})))
        result (g/incoming-refs d 1)]
    (is (= #{2 3} result))))

(deftest incoming-refs-empty-when-not-referenced
  (let [d (-> (c/make-db)
              (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")})))]
    (is (empty? (g/incoming-refs d 1)))))
