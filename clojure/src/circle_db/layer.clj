(ns circle-db.layer
  (:require [circle-db.constructs :as c]
            [circle-db.storage :as s]
            [circle-db.eavt :as e]
            [circle-db.avet :as av]
            [circle-db.veat :as ve]
            [circle-db.vaet :as vae]))

(defn- iter-vals [value]
  (if (set? value) value [value]))

(defn add-entity-to-layer [layer entity]
  (reduce (fn [l [attr-name attr]]
            (let [eavt-datom (c/->Datom (:id entity) attr-name (:value attr))
                  l-with-eavt (update l :eavt e/index-add eavt-datom)]
              (reduce (fn [l v]
                        (let [datom (c/->Datom (:id entity) attr-name v)]
                          (-> l
                              (update :avet  av/index-add datom)
                              (update :veat  ve/index-add datom)
                              (cond-> (= (:type attr) :db/ref)
                                (update :vaet vae/index-add datom)))))
                      l-with-eavt
                      (iter-vals (:value attr)))))
          (update layer :storage s/add-entity entity)
          (:attrs entity)))
