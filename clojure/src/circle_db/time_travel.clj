(ns circle-db.time-travel)

(defn advance-time [db]
  (update db :curr-time inc))

(defn evolution-of [db entity-id attr-name]
  (let [entity (get (:storage (last (:layers db))) entity-id)
        attr   (when entity (get (:attrs entity) attr-name))]
    (when (and attr (not= (:ts attr) -1))
      (loop [current attr
             history  []]
        (let [history' (conj history [(:ts current) (:value current)])
              prev-ts  (:prev-ts current)]
          (if (= prev-ts -1)
            (reverse history')
            (let [prev-attr (some (fn [layer]
                                    (let [e (get (:storage layer) entity-id)]
                                      (when e
                                        (let [a (get (:attrs e) attr-name)]
                                          (when (and a (= (:ts a) prev-ts)) a)))))
                                  (reverse (:layers db)))]
              (if prev-attr
                (recur prev-attr history')
                (reverse history')))))))))

(defn db-at [db t]
  (let [result-idx
        (reduce (fn [best [i layer]]
                  (let [max-ts (reduce (fn [m entity]
                                         (reduce (fn [m2 attr]
                                                   (if (>= (:ts attr) 0) (max m2 (:ts attr)) m2))
                                                 m
                                                 (vals (:attrs entity))))
                                       -1
                                       (vals (:storage layer)))]
                    (if (<= max-ts t) i best)))
                0
                (map-indexed vector (:layers db)))]
    (assoc db :layers (vec (take (inc result-idx) (:layers db))))))
