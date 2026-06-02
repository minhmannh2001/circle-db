# AVET: attribute → value → set of entity_ids
# Fast for: "which entities have attribute A with value V?"

def index_add(index, datom):
    attr = index.get(datom.attr_name, {})
    existing = attr.get(datom.value, set())
    return {**index, datom.attr_name: {**attr, datom.value: existing | {datom.entity_id}}}


def index_remove(index, datom):
    attr = index.get(datom.attr_name, {})
    new_set = attr.get(datom.value, set()) - {datom.entity_id}
    return {**index, datom.attr_name: {**attr, datom.value: new_set}}


def index_get(index, attr_name, value=None):
    attr = dict(index.get(attr_name, {}))
    if value is None:
        return attr
    return attr.get(value, set())
