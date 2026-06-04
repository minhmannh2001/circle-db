(ns circle-db.layer-update
  (:require [clojure.set :as set]
            [circle-db.constructs :as c]
            [circle-db.eavt :as e]
            [circle-db.avet :as av]
            [circle-db.veat :as ve]
            [circle-db.vaet :as vae]))

(defn- iter-vals [value]
  (if (set? value) value [value]))

(defn- apply-op [old-val new-val op cardinality]
  (if (= cardinality :db/single)
    new-val
    (case op
      :reset  new-val
      :add    (set/union old-val new-val)
      :remove (set/difference old-val new-val))))

(defn update-entity-in-layer [layer entity-id attr-name new-val op curr-time]
  (if-let [entity (get (:storage layer) entity-id)]
    (let [old-attr  (get-in entity [:attrs attr-name])
          new-value (apply-op (:value old-attr) new-val op (:cardinality old-attr))
          new-attr  (assoc old-attr :value new-value :ts curr-time :prev-ts (:ts old-attr))
          new-entity (assoc-in entity [:attrs attr-name] new-attr)
          remove-from-indexes (fn [l v]
                                (let [d (c/->Datom entity-id attr-name v)]
                                  (-> l
                                      (update :avet av/index-remove d)
                                      (update :veat ve/index-remove d)
                                      (cond-> (= (:type old-attr) :db/ref)
                                        (update :vaet vae/index-remove d)))))
          add-to-indexes (fn [l v]
                           (let [d (c/->Datom entity-id attr-name v)]
                             (-> l
                                 (update :avet av/index-add d)
                                 (update :veat ve/index-add d)
                                 (cond-> (= (:type old-attr) :db/ref)
                                   (update :vaet vae/index-add d)))))]
      (-> layer
          (assoc-in [:storage entity-id] new-entity)
          (update :eavt e/index-add (c/->Datom entity-id attr-name new-value))
          (as-> l (reduce remove-from-indexes l (iter-vals (:value old-attr))))
          (as-> l (reduce add-to-indexes      l (iter-vals new-value)))))
    layer))
