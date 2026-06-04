(ns circle-db.db
  (:require [circle-db.layer :as l]
            [circle-db.layer-remove :as lr]
            [circle-db.layer-update :as lu]))

(defn add-entity [db entity]
  (let [new-id (inc (:top-id db))
        fixed-entity (assoc entity :id new-id)
        new-layer (l/add-entity-to-layer (last (:layers db)) fixed-entity)]
    (assoc db :layers (conj (:layers db) new-layer) :top-id new-id)))

(defn update-entity
  ([db ent-id attr-name new-val]
   (update-entity db ent-id attr-name new-val :reset))
  ([db ent-id attr-name new-val op]
   (let [new-layer (lu/update-entity-in-layer (last (:layers db)) ent-id attr-name new-val op (:curr-time db))]
     (assoc db :layers (conj (:layers db) new-layer)))))

(defn remove-entity [db ent-id]
  (let [back-refs (get (:vaet (last (:layers db))) ent-id {})
        referencing (set (mapcat val back-refs))]
    (when (seq referencing)
      (throw (ex-info (str "Entity " ent-id " still referenced by entities " referencing) {})))
    (let [new-layer (lr/remove-entity-from-layer (last (:layers db)) ent-id)]
      (assoc db :layers (conj (:layers db) new-layer)))))
