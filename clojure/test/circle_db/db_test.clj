(ns circle-db.db-test
  (:require [clojure.test :refer [deftest is]]
            [circle-db.constructs :as c]
            [circle-db.db :as db]))

(defn- str-attr [value]
  (c/make-attr :name value :db/string))

(defn- multi-attr [value]
  (c/make-attr :tags value :db/string :cardinality :db/multiple))

;; --- add-entity ---

(deftest add-entity-appends-new-layer-and-assigns-id
  (let [db (c/make-db)
        entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")})
        new-db (db/add-entity db entity)]
    (is (= (inc (count (:layers db))) (count (:layers new-db))))
    (is (= 1 (:top-id new-db)))))

(deftest add-entity-entity-is-in-storage-of-new-layer
  (let [db (c/make-db)
        entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")})
        new-db (db/add-entity db entity)]
    (is (= "Alice" (get-in (last (:layers new-db)) [:storage 1 :attrs :name :value])))))

(deftest add-entity-all-four-indexes-updated
  (let [db (c/make-db)
        entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")})
        new-db (db/add-entity db entity)
        layer (last (:layers new-db))]
    (is (= "Alice" (get-in (:eavt layer) [1 :name])))
    (is (= #{1}    (get-in (:avet layer) [:name "Alice"])))
    (is (= #{:name} (get-in (:veat layer) ["Alice" 1])))))

(deftest add-entity-two-entities-both-in-new-layer
  (let [db (c/make-db)
        e1 (assoc (c/make-entity) :attrs {:name (str-attr "Alice")})
        e2 (assoc (c/make-entity) :attrs {:name (str-attr "Bob")})
        new-db (-> db (db/add-entity e1) (db/add-entity e2))
        storage (:storage (last (:layers new-db)))]
    (is (contains? storage 1))
    (is (contains? storage 2))))

(deftest add-entity-original-db-unchanged
  (let [db (c/make-db)
        entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")})
        original-count (count (:layers db))
        original-top-id (:top-id db)
        _ (db/add-entity db entity)]
    (is (= original-count (count (:layers db))))
    (is (= original-top-id (:top-id db)))))

;; --- remove-entity ---

(deftest remove-entity-entity-absent-from-storage
  (let [db (-> (c/make-db)
               (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")})))
        new-db (db/remove-entity db 1)]
    (is (not (contains? (:storage (last (:layers new-db))) 1)))))

(deftest remove-entity-absent-from-all-indexes
  (let [db (-> (c/make-db)
               (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")})))
        new-db (db/remove-entity db 1)
        layer (last (:layers new-db))]
    (is (not (contains? (:eavt layer) 1)))
    (is (= #{} (get-in (:avet layer) [:name "Alice"] #{})))
    (is (= #{} (get-in (:veat layer) ["Alice" 1] #{})))))

(deftest remove-entity-succeeds-after-referencing-entity-removed
  (let [ref (c/make-attr :friend 1 :db/ref)
        db (-> (c/make-db)
               (db/add-entity (c/make-entity))
               (db/add-entity (assoc (c/make-entity) :attrs {:friend ref}))
               (db/remove-entity 2))
        new-db (db/remove-entity db 1)]
    (is (not (contains? (:storage (last (:layers new-db))) 1)))))

(deftest remove-entity-raises-if-still-referenced
  (let [ref (c/make-attr :friend 1 :db/ref)
        db (-> (c/make-db)
               (db/add-entity (c/make-entity))
               (db/add-entity (assoc (c/make-entity) :attrs {:friend ref})))]
    (is (thrown-with-msg? Exception #"still referenced"
          (db/remove-entity db 1)))))

;; --- update-entity ---

(deftest update-entity-single-replaces-value-and-updates-timestamps
  (let [db (-> (c/make-db)
               (db/add-entity (assoc (c/make-entity) :attrs {:name (str-attr "Alice")})))
        old-ts (get-in (last (:layers db)) [:storage 1 :attrs :name :ts])
        new-db (db/update-entity db 1 :name "Bob")
        updated (get-in (last (:layers new-db)) [:storage 1 :attrs :name])]
    (is (= "Bob" (:value updated)))
    (is (= (:curr-time db) (:ts updated)))
    (is (= old-ts (:prev-ts updated)))))

(deftest update-entity-multiple-add-appends-to-set
  (let [attr (c/make-attr :tags #{"python"} :db/string :cardinality :db/multiple)
        db (-> (c/make-db)
               (db/add-entity (assoc (c/make-entity) :attrs {:tags attr})))
        new-db (db/update-entity db 1 :tags #{"clojure"} :add)]
    (is (= #{"python" "clojure"}
           (get-in (last (:layers new-db)) [:storage 1 :attrs :tags :value])))))

(deftest update-entity-multiple-remove-removes-from-set
  (let [attr (c/make-attr :tags #{"python" "clojure"} :db/string :cardinality :db/multiple)
        db (-> (c/make-db)
               (db/add-entity (assoc (c/make-entity) :attrs {:tags attr})))
        new-db (db/update-entity db 1 :tags #{"clojure"} :remove)]
    (is (= #{"python"}
           (get-in (last (:layers new-db)) [:storage 1 :attrs :tags :value])))))
