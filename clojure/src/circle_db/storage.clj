(ns circle-db.storage)

(defn add-entity [storage entity]
  (assoc storage (:id entity) entity))

(defn get-entity [storage entity-id]
  (get storage entity-id))

(defn remove-entity [storage entity-id]
  (dissoc storage entity-id))
