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
