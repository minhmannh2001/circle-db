from dataclasses import replace


def advance_time(db):
    return replace(db, curr_time=db.curr_time + 1)


def evolution_of(db, entity_id, attr_name):
    entity = db.layers[-1].storage.get(entity_id)
    if not entity:
        return []
    attr = entity.attrs.get(attr_name)
    if not attr or attr.ts == -1:
        return []

    history = []
    while attr and attr.ts != -1:
        history.append((attr.ts, attr.value))
        prev_ts = attr.prev_ts
        if prev_ts == -1:
            break
        attr = None
        for layer in reversed(db.layers):
            candidate = layer.storage.get(entity_id)
            if candidate:
                a = candidate.attrs.get(attr_name)
                if a and a.ts == prev_ts:
                    attr = a
                    break

    history.reverse()
    return history


def db_at(db, t):
    result_idx = 0
    for i, layer in enumerate(db.layers):
        layer_max_ts = max(
            (a.ts for e in layer.storage.values() for a in e.attrs.values() if a.ts >= 0),
            default=-1,
        )
        if layer_max_ts <= t:
            result_idx = i
    return replace(db, layers=db.layers[:result_idx + 1])
