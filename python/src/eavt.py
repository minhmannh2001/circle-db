def index_add(index, datom):
    entity = index.get(datom.entity_id, {})
    return {**index, datom.entity_id: {**entity, datom.attr_name: datom.value}}


def index_remove(index, datom):
    entity = index.get(datom.entity_id, {})
    new_entity = {k: v for k, v in entity.items() if k != datom.attr_name}
    return {**index, datom.entity_id: new_entity}


def index_get(index, entity_id, attr_name=None):
    entity = dict(index.get(entity_id, {}))
    if attr_name is None:
        return entity
    return entity.get(attr_name)
