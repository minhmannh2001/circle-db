from dataclasses import replace
from layer import add_entity_to_layer
from layer_remove import remove_entity_from_layer
from layer_update import update_entity_in_layer


def add_entity(db, entity):
    new_id = db.top_id + 1
    fixed_entity = replace(entity, id=new_id)
    new_layer = add_entity_to_layer(db.layers[-1], fixed_entity)
    return replace(db, layers=db.layers + [new_layer], top_id=new_id)


def update_entity(db, ent_id, attr_name, new_val, op="reset"):
    new_layer = update_entity_in_layer(db.layers[-1], ent_id, attr_name, new_val, op, db.curr_time)
    return replace(db, layers=db.layers + [new_layer])


def remove_entity(db, ent_id):
    back_refs = db.layers[-1].vaet.get(ent_id, {})
    referencing = {e for entities in back_refs.values() for e in entities}
    if referencing:
        raise ValueError(f"Entity {ent_id} still referenced by entities {referencing}")
    new_layer = remove_entity_from_layer(db.layers[-1], ent_id)
    return replace(db, layers=db.layers + [new_layer])
    
