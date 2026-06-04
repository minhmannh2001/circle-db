from dataclasses import replace
from constructs import Datom
from storage import remove_entity as storage_remove
from eavt import index_remove as eavt_remove
from avet import index_remove as avet_remove
from veat import index_remove as veat_remove
from vaet import index_remove as vaet_remove


def remove_entity_from_layer(layer, entity_id):
    entity = layer.storage.get(entity_id)
    if entity is None:
        return layer
    new_storage = storage_remove(layer.storage, entity_id)
    new_eavt, new_avet, new_veat, new_vaet = layer.eavt, layer.avet, layer.veat, layer.vaet
    for attr_name, attr in entity.attrs.items():
        new_eavt = eavt_remove(new_eavt, Datom(entity_id=entity_id, attr_name=attr_name, value=attr.value))
        values = attr.value if isinstance(attr.value, set) else [attr.value]
        for v in values:
            datom = Datom(entity_id=entity_id, attr_name=attr_name, value=v)
            new_avet = avet_remove(new_avet, datom)
            new_veat = veat_remove(new_veat, datom)
            if attr.type == ":db/ref":
                new_vaet = vaet_remove(new_vaet, datom)
    # clean up empty eavt entry and VAET back-refs pointing to this entity
    new_eavt = {k: v for k, v in new_eavt.items() if k != entity_id}
    new_vaet = {k: v for k, v in new_vaet.items() if k != entity_id}
    return replace(layer, storage=new_storage, eavt=new_eavt, avet=new_avet, veat=new_veat, vaet=new_vaet)
