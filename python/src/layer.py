from dataclasses import replace
from constructs import Datom, Layer
from storage import add_entity
from eavt import index_add as eavt_add
from avet import index_add as avet_add
from veat import index_add as veat_add
from vaet import index_add as vaet_add


def _iter_values(value):
    return value if isinstance(value, set) else [value]


def add_entity_to_layer(layer, entity):
    new_storage = add_entity(layer.storage, entity)
    new_eavt, new_avet, new_veat, new_vaet = layer.eavt, layer.avet, layer.veat, layer.vaet
    for attr_name, attr in entity.attrs.items():
        new_eavt = eavt_add(new_eavt, Datom(entity_id=entity.id, attr_name=attr_name, value=attr.value))
        for v in _iter_values(attr.value):
            datom = Datom(entity_id=entity.id, attr_name=attr_name, value=v)
            new_avet = avet_add(new_avet, datom)
            new_veat = veat_add(new_veat, datom)
            if attr.type == ":db/ref":
                new_vaet = vaet_add(new_vaet, datom)
    return replace(layer, storage=new_storage, eavt=new_eavt, avet=new_avet, veat=new_veat, vaet=new_vaet)
