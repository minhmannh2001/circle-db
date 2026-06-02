(ns circle-db.veat)
;; VEAT: value → entity-id → set of attribute names
;; Fast for: "which entities have this value?"

(defn index-add [index datom]
  (update-in index [(:value datom) (:entity-id datom)] (fnil conj #{}) (:attr-name datom)))

(defn index-remove [index datom]
  (update-in index [(:value datom) (:entity-id datom)] disj (:attr-name datom)))

(defn index-get
  ([index value]
   (get index value {}))
  ([index value entity-id]
   (get-in index [value entity-id] #{})))
