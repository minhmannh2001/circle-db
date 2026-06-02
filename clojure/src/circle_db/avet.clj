(ns circle-db.avet)
;; AVET: attribute → value → set of entity-ids
;; Fast for: "which entities have attribute A with value V?"

(defn index-add [index datom]
  (update-in index [(:attr-name datom) (:value datom)] (fnil conj #{}) (:entity-id datom)))

(defn index-remove [index datom]
  (update-in index [(:attr-name datom) (:value datom)] disj (:entity-id datom)))

(defn index-get
  ([index attr-name]
   (get index attr-name {}))
  ([index attr-name value]
   (get-in index [attr-name value] #{})))
