(ns circle-db.indexes-test
  (:require [clojure.test :refer [deftest is]]
            [circle-db.constructs :as c]
            [circle-db.avet :as av]
            [circle-db.veat :as ve]
            [circle-db.vaet :as vae]
            [circle-db.eavt :as e]
            [circle-db.layer :as l]))

(deftest avet-returns-set-of-entity-ids
  (let [d (c/->Datom 1 :name "Alice")
        avet (av/index-add {} d)]
    (is (= #{1} (av/index-get avet :name "Alice")))))

(deftest avet-two-entities-same-value-both-in-set
  (let [avet (-> {} (av/index-add (c/->Datom 1 :name "Alice"))
                    (av/index-add (c/->Datom 5 :name "Alice")))]
    (is (= #{1 5} (av/index-get avet :name "Alice")))))

(deftest veat-returns-set-of-attr-names
  (let [d (c/->Datom 1 :name "Alice")
        veat (ve/index-add {} d)]
    (is (= #{:name} (ve/index-get veat "Alice" 1)))))

(deftest vaet-returns-set-of-entity-ids
  (let [d (c/->Datom 1 :friend 42)
        vaet (vae/index-add {} d)]
    (is (= #{1} (vae/index-get vaet 42 :friend)))))

(deftest vaet-two-entities-same-ref-both-in-set
  (let [vaet (-> {} (vae/index-add (c/->Datom 1 :friend 42))
                    (vae/index-add (c/->Datom 3 :friend 42)))]
    (is (= #{1 3} (vae/index-get vaet 42 :friend)))))

(deftest layer-vaet-populated-only-for-ref-attrs
  (let [ref-attr (c/make-attr :friend 42 :db/ref)
        str-attr (c/make-attr :name "Alice" :db/string)
        entity   (c/->Entity 1 {:friend ref-attr :name str-attr})
        new-layer (l/add-entity-to-layer (c/make-layer) entity)]
    (is (= #{1} (vae/index-get (:vaet new-layer) 42 :friend)))
    (is (nil? (get (:vaet new-layer) "Alice")))))

(deftest layer-all-four-indexes-populated
  (let [attr (c/make-attr :name "Alice" :db/string)
        entity (c/->Entity 1 {:name attr})
        new-layer (l/add-entity-to-layer (c/make-layer) entity)]
    (is (= entity   (get (:storage new-layer) 1)))
    (is (= "Alice"  (e/index-get  (:eavt new-layer) 1 :name)))
    (is (= #{1}     (av/index-get (:avet new-layer) :name "Alice")))
    (is (= #{:name} (ve/index-get (:veat new-layer) "Alice" 1)))))

(deftest avet-partial-get-returns-all-values-for-attr
  (let [avet (-> {} (av/index-add (c/->Datom 1 :name "Alice"))
                    (av/index-add (c/->Datom 5 :name "Bob")))]
    (is (= {"Alice" #{1} "Bob" #{5}} (av/index-get avet :name)))))

(deftest veat-partial-get-returns-all-entities-for-value
  (let [veat (-> {} (ve/index-add (c/->Datom 1 :name "Alice"))
                    (ve/index-add (c/->Datom 7 :nickname "Alice")))]
    (is (= {1 #{:name} 7 #{:nickname}} (ve/index-get veat "Alice")))))

(deftest vaet-partial-get-returns-all-attrs-for-ref
  (let [vaet (-> {} (vae/index-add (c/->Datom 1 :friend 42))
                    (vae/index-add (c/->Datom 3 :mentor 42)))]
    (is (= {:friend #{1} :mentor #{3}} (vae/index-get vaet 42)))))
