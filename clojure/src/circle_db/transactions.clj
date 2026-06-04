(ns circle-db.transactions)

(defn- apply-ops [db ops]
  (reduce (fn [d op] (op d)) db ops))

(defn transact [db-atom ops]
  (swap! db-atom
         (fn [db]
           (let [working (apply-ops db ops)
                 final-layer (last (:layers working))]
             (assoc db
                    :layers (conj (:layers db) final-layer)
                    :top-id (:top-id working)
                    :curr-time (inc (:curr-time db)))))))

(defn what-if [db ops]
  (let [working (apply-ops db ops)
        final-layer (last (:layers working))]
    (assoc db
           :layers (conj (:layers db) final-layer)
           :top-id (:top-id working))))
