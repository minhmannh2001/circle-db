from constructs import Datom, Attribute, Entity, Layer
from avet import index_add as avet_add, index_get as avet_get
from veat import index_add as veat_add, index_get as veat_get
from vaet import index_add as vaet_add, index_get as vaet_get
from layer import add_entity_to_layer
from eavt import index_get as eavt_get


def test_avet_add_then_get_entity_by_attr_and_value():
    d = Datom(entity_id=1, attr_name="name", value="Alice")
    avet = avet_add({}, d)
    assert avet_get(avet, "name", "Alice") == {1}


def test_avet_two_entities_same_value_both_in_set():
    d1 = Datom(entity_id=1, attr_name="name", value="Alice")
    d2 = Datom(entity_id=5, attr_name="name", value="Alice")
    avet = avet_add(avet_add({}, d1), d2)
    assert avet_get(avet, "name", "Alice") == {1, 5}


def test_veat_add_then_get_attr_by_value_and_entity():
    d = Datom(entity_id=1, attr_name="name", value="Alice")
    veat = veat_add({}, d)
    assert veat_get(veat, "Alice", 1) == {"name"}


def test_vaet_add_ref_datom_is_indexed():
    d = Datom(entity_id=1, attr_name="friend", value=42)
    vaet = vaet_add({}, d)
    assert vaet_get(vaet, 42, "friend") == {1}


def test_vaet_two_entities_same_ref_both_in_set():
    d1 = Datom(entity_id=1, attr_name="friend", value=42)
    d2 = Datom(entity_id=3, attr_name="friend", value=42)
    vaet = vaet_add(vaet_add({}, d1), d2)
    assert vaet_get(vaet, 42, "friend") == {1, 3}


def test_layer_vaet_populated_only_for_ref_attrs():
    ref_attr = Attribute(name="friend", value=42, type=":db/ref", cardinality=":db/single")
    str_attr = Attribute(name="name",   value="Alice", type=":db/string", cardinality=":db/single")
    entity = Entity(id=1, attrs={"friend": ref_attr, "name": str_attr})
    new_layer = add_entity_to_layer(Layer(), entity)
    assert vaet_get(new_layer.vaet, 42, "friend") == {1}
    assert new_layer.vaet.get("Alice") is None


def test_layer_all_four_indexes_populated():
    attr = Attribute(name="name", value="Alice", type=":db/string", cardinality=":db/single")
    entity = Entity(id=1, attrs={"name": attr})
    new_layer = add_entity_to_layer(Layer(), entity)
    assert new_layer.storage[1] is entity
    assert eavt_get(new_layer.eavt, 1, "name") == "Alice"
    assert avet_get(new_layer.avet, "name", "Alice") == {1}
    assert veat_get(new_layer.veat, "Alice", 1) == {"name"}


def test_avet_partial_get_returns_all_values_for_attr():
    d1 = Datom(entity_id=1, attr_name="name", value="Alice")
    d2 = Datom(entity_id=5, attr_name="name", value="Bob")
    avet = avet_add(avet_add({}, d1), d2)
    assert avet_get(avet, "name") == {"Alice": {1}, "Bob": {5}}


def test_veat_partial_get_returns_all_entities_for_value():
    d1 = Datom(entity_id=1, attr_name="name", value="Alice")
    d2 = Datom(entity_id=7, attr_name="nickname", value="Alice")
    veat = veat_add(veat_add({}, d1), d2)
    assert veat_get(veat, "Alice") == {1: {"name"}, 7: {"nickname"}}


def test_vaet_partial_get_returns_all_attrs_for_ref():
    d1 = Datom(entity_id=1, attr_name="friend", value=42)
    d2 = Datom(entity_id=3, attr_name="mentor", value=42)
    vaet = vaet_add(vaet_add({}, d1), d2)
    assert vaet_get(vaet, 42) == {"friend": {1}, "mentor": {3}}
