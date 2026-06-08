(ns circle-db.graph)

(defn outgoing-refs [db entity-id]
  (let [entity (get (:storage (last (:layers db))) entity-id)]
    (when entity
      (mapcat (fn [attr]
                (when (= (:type attr) :db/ref)
                  (if (set? (:value attr))
                    (:value attr)
                    [(:value attr)])))
              (vals (:attrs entity))))))

(defn incoming-refs [db entity-id]
  (let [back-refs (get (:vaet (last (:layers db))) entity-id {})]
    (set (mapcat val back-refs))))
