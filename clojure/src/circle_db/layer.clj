(ns circle-db.layer
  (:require [circle-db.constructs :as c]
            [circle-db.storage :as s]
            [circle-db.eavt :as e]))

(defn add-entity-to-layer [layer entity]
  (let [new-storage (s/add-entity (:storage layer) entity)
        new-eavt (reduce (fn [idx [attr-name attr]]
                           (e/index-add idx (c/->Datom (:id entity) attr-name (:value attr))))
                         (:eavt layer)
                         (:attrs entity))]
    (assoc layer :storage new-storage :eavt new-eavt)))
