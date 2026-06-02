(ns circle-db.layer
  (:require [circle-db.constructs :as c]
            [circle-db.storage :as s]
            [circle-db.eavt :as e]
            [circle-db.avet :as av]
            [circle-db.veat :as ve]
            [circle-db.vaet :as vae]))

(defn add-entity-to-layer [layer entity]
  (reduce (fn [l [attr-name attr]]
            (let [datom (c/->Datom (:id entity) attr-name (:value attr))]
              (-> l
                  (update :eavt  e/index-add  datom)
                  (update :avet  av/index-add datom)
                  (update :veat  ve/index-add datom)
                  (cond-> (= (:type attr) :db/ref)
                    (update :vaet vae/index-add datom)))))
          (update layer :storage s/add-entity entity)
          (:attrs entity)))
