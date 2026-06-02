from dataclasses import replace
from constructs import Datom, Layer
from storage import add_entity
from eavt import index_add


def add_entity_to_layer(layer, entity):
    new_storage = add_entity(layer.storage, entity)
    new_eavt = layer.eavt
    for attr_name, attr in entity.attrs.items():
        datom = Datom(entity_id=entity.id, attr_name=attr_name, value=attr.value)
        new_eavt = index_add(new_eavt, datom)
    return replace(layer, storage=new_storage, eavt=new_eavt)
