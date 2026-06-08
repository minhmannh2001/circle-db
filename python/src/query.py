from collections.abc import Callable


def _is_var(x):
    return isinstance(x, str) and x.startswith("?")


def transform(clause):
    entity, attr, value = clause

    def predicate(datom):
        if not _is_var(entity) and datom.entity_id != entity:
            return False
        if not _is_var(attr) and datom.attr_name != attr:
            return False
        if not _is_var(value) and datom.value != value:
            return False
        return True

    return predicate


def plan(clauses, layer) -> Callable:
    clause = clauses[0]
    entity, attr, value = clause
    ev, av, vv = _is_var(entity), _is_var(attr), _is_var(value)

    # Point lookups — 1 variable, 2 known constants
    if ev and not av and not vv:
        return lambda: layer.avet.get(attr, {}).get(value, set())
    if vv and not ev and not av:
        return lambda: layer.eavt.get(entity, {}).get(attr)
    if av and not ev and not vv:
        return lambda: layer.veat.get(value, {}).get(entity, set())

    # Range scans — 2 variables, 1 known constant
    if av and vv:
        return lambda: dict(layer.eavt.get(entity, {}))
    if ev and vv:
        return lambda: dict(layer.avet.get(attr, {}))
    if ev and av:
        return lambda: dict(layer.veat.get(value, {}))

    raise ValueError(f"Unsupported clause pattern: {clause}")


def execute(clauses, layer):
    entity_var = None
    entity_sets = []
    var_bindings = {}  # entity_id → [{var_name: value}, ...]  (list to support multiple rows per entity)

    for clause in clauses:
        e, attr, v = clause
        if not _is_var(e):
            continue
        entity_var = e
        plan_fn = plan([clause], layer)
        raw = plan_fn()

        if not _is_var(v) and not _is_var(attr):
            # Point lookup: raw is a set of entity IDs
            # e.g. ["?e", "name", "Alice"] → avet["name"]["Alice"] = {1, 3}
            entity_sets.append(raw if raw else set())

        elif _is_var(v) and not _is_var(attr):
            # AVET range scan: raw is {val: {entity_ids}}
            # e.g. ["?e", "name", "?name"] → {"Alice": {1,3}, "Bob": {2}}
            ids = set()
            if raw:
                for val, eids in raw.items():
                    for eid in eids:
                        if eid in var_bindings:
                            var_bindings[eid] = [{**b, v: val} for b in var_bindings[eid]]
                        else:
                            var_bindings[eid] = [{v: val}]
                        ids.add(eid)
            entity_sets.append(ids)

        elif _is_var(attr) and not _is_var(v):
            # VEAT range scan: raw is {entity_id: {attr_names}}
            # e.g. ["?e", "?a", "Alice"] → {1: {"name"}, 3: {"name"}}
            ids = set()
            if raw:
                for eid, attrs in raw.items():
                    new_partials = [{attr: a} for a in attrs]
                    if eid in var_bindings:
                        var_bindings[eid] = [
                            {**existing, **new_p}
                            for existing in var_bindings[eid]
                            for new_p in new_partials
                        ]
                    else:
                        var_bindings[eid] = new_partials
                    ids.add(eid)
            entity_sets.append(ids)

    if not entity_sets:
        return []

    surviving_ids = set.intersection(*entity_sets)

    result = []
    for eid in surviving_ids:
        base = {entity_var: eid}
        for partial in var_bindings.get(eid, [{}]):
            result.append({**base, **partial})
    return result


def unify(bindings, find_vars):
    return [[b[v] for v in find_vars] for b in bindings]


def q(query, db):
    layer = db.layers[-1]
    bindings = execute(query["where"], layer)
    return unify(bindings, query["find"])
