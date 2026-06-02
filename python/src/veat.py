# VEAT: value → entity_id → set of attribute names
# Fast for: "which entities have this value?"

def index_add(index, datom):
    val = index.get(datom.value, {})
    existing = val.get(datom.entity_id, set())
    return {**index, datom.value: {**val, datom.entity_id: existing | {datom.attr_name}}}


def index_remove(index, datom):
    val = index.get(datom.value, {})
    new_set = val.get(datom.entity_id, set()) - {datom.attr_name}
    return {**index, datom.value: {**val, datom.entity_id: new_set}}


def index_get(index, value, entity_id=None):
    val = dict(index.get(value, {}))
    if entity_id is None:
        return val
    return val.get(entity_id, set())
