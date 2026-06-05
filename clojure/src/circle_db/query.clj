(ns circle-db.query)

(defn- qvar? [x]
  (and (string? x) (.startsWith x "?")))

(defn transform [[entity attr value]]
  (fn [datom]
    (and (or (qvar? entity) (= (:entity-id datom) entity))
         (or (qvar? attr)   (= (:attr-name datom) attr))
         (or (qvar? value)  (= (:value datom) value)))))

(defn plan [[clause & _] layer]
  (let [[entity attr value] clause
        ev (qvar? entity)
        av (qvar? attr)
        vv (qvar? value)]
    (cond
      ;; Point lookups — 1 variable, 2 known
      (and ev (not av) (not vv)) (fn [] (get-in layer [:avet attr value] #{}))
      (and vv (not ev) (not av)) (fn [] (get-in layer [:eavt entity attr]))
      (and av (not ev) (not vv)) (fn [] (get-in layer [:veat value entity] #{}))
      ;; Range scans — 2 variables, 1 known
      (and av vv)                (fn [] (into {} (get-in layer [:eavt entity] {})))
      (and ev vv)                (fn [] (into {} (get-in layer [:avet attr] {})))
      (and ev av)                (fn [] (into {} (get-in layer [:veat value] {}))))))
