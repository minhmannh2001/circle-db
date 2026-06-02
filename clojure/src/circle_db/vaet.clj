(ns circle-db.vaet)
;; VAET: value → attribute → set of entity-ids  (only for :db/ref attributes)
;; Fast for: "which entities reference entity X?"

(defn index-add [index datom]
  (update-in index [(:value datom) (:attr-name datom)] (fnil conj #{}) (:entity-id datom)))

(defn index-remove [index datom]
  (update-in index [(:value datom) (:attr-name datom)] disj (:entity-id datom)))

(defn index-get
  ([index value]
   (get index value {}))
  ([index value attr-name]
   (get-in index [value attr-name] #{})))
