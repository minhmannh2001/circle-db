# VAET: value → attribute → set of entity_ids  (only for :db/ref attributes)
# Fast for: "which entities reference entity X?"

def index_add(index, datom):
    val = index.get(datom.value, {})
    existing = val.get(datom.attr_name, set())
    return {**index, datom.value: {**val, datom.attr_name: existing | {datom.entity_id}}}


def index_remove(index, datom):
    val = index.get(datom.value, {})
    new_set = val.get(datom.attr_name, set()) - {datom.entity_id}
    return {**index, datom.value: {**val, datom.attr_name: new_set}}


def index_get(index, value, attr_name=None):
    val = dict(index.get(value, {}))
    if attr_name is None:
        return val
    return val.get(attr_name, set())
