from dataclasses import replace
from constructs import Datom, Layer
from storage import add_entity
from eavt import index_add as eavt_add
from avet import index_add as avet_add
from veat import index_add as veat_add
from vaet import index_add as vaet_add


def add_entity_to_layer(layer, entity):
    new_storage = add_entity(layer.storage, entity)
    new_eavt, new_avet, new_veat, new_vaet = layer.eavt, layer.avet, layer.veat, layer.vaet
    for attr_name, attr in entity.attrs.items():
        datom = Datom(entity_id=entity.id, attr_name=attr_name, value=attr.value)
        new_eavt = eavt_add(new_eavt, datom)
        new_avet = avet_add(new_avet, datom)
        new_veat = veat_add(new_veat, datom)
        if attr.type == ":db/ref":
            new_vaet = vaet_add(new_vaet, datom)
    return replace(layer, storage=new_storage, eavt=new_eavt, avet=new_avet, veat=new_veat, vaet=new_vaet)
