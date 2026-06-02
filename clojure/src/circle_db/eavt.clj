(ns circle-db.eavt)

(defn index-add [index datom]
  (assoc-in index [(:entity-id datom) (:attr-name datom)] (:value datom)))

(defn index-remove [index datom]
  (update-in index [(:entity-id datom)] dissoc (:attr-name datom)))

(defn index-get
  ([index entity-id]
   (get index entity-id {}))
  ([index entity-id attr-name]
   (get-in index [entity-id attr-name])))
