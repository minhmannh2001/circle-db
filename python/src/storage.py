def add_entity(storage, entity):
    return {**storage, entity.id: entity}


def get_entity(storage, entity_id):
    return storage.get(entity_id)


def remove_entity(storage, entity_id):
    return {k: v for k, v in storage.items() if k != entity_id}
