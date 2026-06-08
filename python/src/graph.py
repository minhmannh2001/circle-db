def outgoing_refs(db, entity_id):
    entity = db.layers[-1].storage.get(entity_id)
    if not entity:
        return []
    refs = []
    for attr in entity.attrs.values():
        if attr.type == ":db/ref":
            if isinstance(attr.value, set):
                refs.extend(attr.value)
            else:
                refs.append(attr.value)
    return refs


def incoming_refs(db, entity_id):
    back_refs = db.layers[-1].vaet.get(entity_id, {})
    return list({e for eids in back_refs.values() for e in eids})
