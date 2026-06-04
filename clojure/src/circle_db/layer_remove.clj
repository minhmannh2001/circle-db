(ns circle-db.layer-remove
  (:require [circle-db.constructs :as c]
            [circle-db.storage :as s]
            [circle-db.avet :as av]
            [circle-db.veat :as ve]
            [circle-db.vaet :as vae]))

(defn- iter-vals [value]
  (if (set? value) value [value]))

(defn remove-entity-from-layer [layer entity-id]
  (if-let [entity (get (:storage layer) entity-id)]
    (-> (reduce (fn [l [attr-name attr]]
                  (reduce (fn [l v]
                            (let [datom (c/->Datom entity-id attr-name v)]
                              (-> l
                                  (update :avet av/index-remove datom)
                                  (update :veat ve/index-remove datom)
                                  (cond-> (= (:type attr) :db/ref)
                                    (update :vaet vae/index-remove datom)))))
                          l
                          (iter-vals (:value attr))))
                layer
                (:attrs entity))
        (update :storage s/remove-entity entity-id)
        (update :eavt dissoc entity-id)
        (update :vaet dissoc entity-id))
    layer))
