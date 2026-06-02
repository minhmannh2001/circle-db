(ns circle-db.constructs)

(defrecord Datom [entity-id attr-name value])

(defrecord Attribute [name value type cardinality ts prev-ts])

(defrecord Entity [id attrs])

(defrecord Layer [storage eavt avet veat vaet])

(defrecord Database [layers top-id curr-time])

(defn make-attr [name value type & {:keys [cardinality] :or {cardinality :db/single}}]
  (->Attribute name value type cardinality -1 -1))

(defn make-entity
  ([]    (->Entity :db/no-id-yet {}))
  ([id]  (->Entity id {})))

(defn make-layer []
  (->Layer {} {} {} {} {}))

(defn make-db []
  (->Database [(make-layer)] 0 0))
