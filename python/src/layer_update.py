from dataclasses import replace
from constructs import Datom
from eavt import index_add as eavt_add
from avet import index_remove as avet_remove, index_add as avet_add
from veat import index_remove as veat_remove, index_add as veat_add
from vaet import index_remove as vaet_remove, index_add as vaet_add


def _apply_op(old_value, new_val, op, cardinality):
    if cardinality == ":db/single":
        return new_val
    if op == "reset":
        return new_val
    if op == "add":
        return (old_value or set()) | new_val
    if op == "remove":
        return (old_value or set()) - new_val


def update_entity_in_layer(layer, entity_id, attr_name, new_val, op, curr_time):
    entity = layer.storage.get(entity_id)
    if entity is None:
        return layer
    old_attr = entity.attrs[attr_name]
    new_value = _apply_op(old_attr.value, new_val, op, old_attr.cardinality)
    new_attr = replace(old_attr, value=new_value, ts=curr_time, prev_ts=old_attr.ts)
    new_entity = replace(entity, attrs={**entity.attrs, attr_name: new_attr})
    new_storage = {**layer.storage, entity_id: new_entity}

    def iter_vals(v):
        return v if isinstance(v, set) else [v]

    new_eavt = eavt_add(layer.eavt, Datom(entity_id=entity_id, attr_name=attr_name, value=new_value))
    new_avet, new_veat, new_vaet = layer.avet, layer.veat, layer.vaet
    for v in iter_vals(old_attr.value):
        d = Datom(entity_id=entity_id, attr_name=attr_name, value=v)
        new_avet = avet_remove(new_avet, d)
        new_veat = veat_remove(new_veat, d)
        if old_attr.type == ":db/ref":
            new_vaet = vaet_remove(new_vaet, d)
    for v in iter_vals(new_value):
        d = Datom(entity_id=entity_id, attr_name=attr_name, value=v)
        new_avet = avet_add(new_avet, d)
        new_veat = veat_add(new_veat, d)
        if old_attr.type == ":db/ref":
            new_vaet = vaet_add(new_vaet, d)

    return replace(layer, storage=new_storage, eavt=new_eavt, avet=new_avet, veat=new_veat, vaet=new_vaet)
