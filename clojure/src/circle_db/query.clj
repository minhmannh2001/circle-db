(ns circle-db.query
  (:require [clojure.set :as set]))

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

(defn execute [clauses layer]
  (let [entity-var (some #(let [e (first %)] (when (qvar? e) e)) clauses)]
    (if (nil? entity-var)
      []
      (let [{:keys [entity-sets var-bindings]}
            (reduce
             (fn [{:keys [entity-sets var-bindings]} [e attr v :as clause]]
               (if-not (qvar? e)
                 {:entity-sets entity-sets :var-bindings var-bindings}
                 (let [plan-fn (plan [clause] layer)
                       raw     (plan-fn)]
                   (cond
                     ;; AVET range scan: raw is {val #{entity-ids}}
                     ;; e.g. ["?e" :name "?name"] → {"Alice" #{1 3} "Bob" #{2}}
                     (and (qvar? v) (not (qvar? attr)))
                     {:entity-sets  (conj entity-sets
                                          (reduce (fn [acc [_ eids]] (into acc eids)) #{} raw))
                      :var-bindings (reduce (fn [vb [val eids]]
                                              (reduce (fn [vb' eid]
                                                        (update vb' eid
                                                                #(if % (mapv (fn [b] (merge b {v val})) %) [{v val}])))
                                                      vb eids))
                                            var-bindings raw)}

                     ;; VEAT range scan: raw is {entity-id #{attr-names}}
                     ;; e.g. ["?e" "?a" "Alice"] → {1 #{:name} 3 #{:name}}
                     (and (qvar? attr) (not (qvar? v)))
                     {:entity-sets  (conj entity-sets (set (keys raw)))
                      :var-bindings (reduce (fn [vb [eid attrs]]
                                              (let [new-ps (mapv (fn [a] {attr a}) attrs)]
                                                (update vb eid
                                                        #(if %
                                                           (for [existing % new-p new-ps]
                                                             (merge existing new-p))
                                                           new-ps))))
                                            var-bindings raw)}

                     ;; Point lookup: raw is #{entity-ids}
                     :else
                     {:entity-sets  (conj entity-sets (or raw #{}))
                      :var-bindings var-bindings}))))
             {:entity-sets [] :var-bindings {}}
             clauses)]
        (when (seq entity-sets)
          (let [surviving (apply set/intersection entity-sets)]
            (for [eid      surviving
                  partial  (or (get var-bindings eid) [{}])]
              (merge {entity-var eid} partial))))))))

(defn unify [bindings find-vars]
  (for [b bindings]
    (mapv #(get b %) find-vars)))

(defn q [query db]
  (let [layer (last (:layers db))]
    (unify (execute (:where query) layer) (:find query))))
