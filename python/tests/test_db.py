from constructs import make_db, Entity, Attribute
from db import add_entity, update_entity, remove_entity


def make_attr(value, type=":db/string", cardinality=":db/single"):
    return Attribute(name="", value=value, type=type, cardinality=cardinality)


# --- add_entity ---

def test_add_entity_appends_new_layer_and_assigns_id():
    db = make_db()
    entity = Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice")})
    new_db = add_entity(db, entity)
    assert len(new_db.layers) == len(db.layers) + 1
    assert new_db.top_id == 1


def test_add_entity_entity_is_in_storage_of_new_layer():
    db = make_db()
    entity = Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice")})
    new_db = add_entity(db, entity)
    assert new_db.layers[-1].storage[1].attrs["name"].value == "Alice"


def test_add_entity_all_four_indexes_updated():
    db = make_db()
    entity = Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice")})
    new_db = add_entity(db, entity)
    layer = new_db.layers[-1]
    assert layer.eavt[1]["name"] == "Alice"
    assert layer.avet["name"]["Alice"] == {1}
    assert layer.veat["Alice"][1] == {"name"}


def test_add_entity_two_entities_both_in_new_layer():
    db = make_db()
    e1 = Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice")})
    e2 = Entity(id=":db/no-id-yet", attrs={"name": make_attr("Bob")})
    new_db = add_entity(add_entity(db, e1), e2)
    layer = new_db.layers[-1]
    assert 1 in layer.storage
    assert 2 in layer.storage


def test_add_entity_original_db_unchanged():
    db = make_db()
    entity = Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice")})
    original_layer_count = len(db.layers)
    original_top_id = db.top_id
    add_entity(db, entity)
    assert len(db.layers) == original_layer_count
    assert db.top_id == original_top_id


# --- remove_entity ---

def test_remove_entity_entity_absent_from_storage():
    db = make_db()
    entity = Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice")})
    db = add_entity(db, entity)
    new_db = remove_entity(db, 1)
    assert 1 not in new_db.layers[-1].storage


def test_remove_entity_absent_from_all_indexes():
    db = make_db()
    entity = Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice")})
    db = add_entity(db, entity)
    new_db = remove_entity(db, 1)
    layer = new_db.layers[-1]
    assert 1 not in layer.eavt
    assert layer.avet.get("name", {}).get("Alice", set()) == set()
    assert layer.veat.get("Alice", {}).get(1, set()) == set()


# --- update_entity ---

def test_update_entity_single_replaces_value_and_updates_timestamps():
    attr = Attribute(name="name", value="Alice", type=":db/string", cardinality=":db/single", ts=0)
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": attr}))
    new_db = update_entity(db, 1, "name", "Bob")
    updated = new_db.layers[-1].storage[1].attrs["name"]
    assert updated.value == "Bob"
    assert updated.ts == db.curr_time
    assert updated.prev_ts == 0


def test_update_entity_multiple_add_appends_to_set():
    attr = Attribute(name="tags", value={"python"}, type=":db/string", cardinality=":db/multiple")
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"tags": attr}))
    new_db = update_entity(db, 1, "tags", {"clojure"}, "add")
    assert new_db.layers[-1].storage[1].attrs["tags"].value == {"python", "clojure"}


def test_update_entity_multiple_remove_removes_from_set():
    attr = Attribute(name="tags", value={"python", "clojure"}, type=":db/string", cardinality=":db/multiple")
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"tags": attr}))
    new_db = update_entity(db, 1, "tags", {"clojure"}, "remove")
    assert new_db.layers[-1].storage[1].attrs["tags"].value == {"python"}


def test_remove_entity_succeeds_after_referencing_entity_removed():
    ref = Attribute(name="friend", value=1, type=":db/ref", cardinality=":db/single")
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={}))                       # id=1
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"friend": ref}))          # id=2, refs e1
    db = remove_entity(db, 2)                                                        # remove referencing first
    new_db = remove_entity(db, 1)                                                    # now safe
    assert 1 not in new_db.layers[-1].storage


def test_remove_entity_raises_if_still_referenced():
    import pytest
    ref = Attribute(name="friend", value=1, type=":db/ref", cardinality=":db/single")
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={}))                       # id=1
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"friend": ref}))          # id=2, refs e1
    with pytest.raises(ValueError, match="still referenced"):
        remove_entity(db, 1)
