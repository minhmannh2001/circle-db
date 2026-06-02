from constructs import Datom, Attribute, Entity, Layer
from eavt import index_add, index_remove, index_get
from layer import add_entity_to_layer


def test_add_datom_then_get_entity_attributes():
    d = Datom(entity_id=1, attr_name="name", value="Alice")
    index = index_add({}, d)
    assert index_get(index, 1) == {"name": "Alice"}


def test_two_datoms_same_entity_both_returned():
    d1 = Datom(entity_id=1, attr_name="name", value="Alice")
    d2 = Datom(entity_id=1, attr_name="age", value=30)
    index = index_add(index_add({}, d1), d2)
    assert index_get(index, 1) == {"name": "Alice", "age": 30}


def test_index_get_specific_attr():
    d1 = Datom(entity_id=1, attr_name="name", value="Alice")
    d2 = Datom(entity_id=1, attr_name="age", value=30)
    index = index_add(index_add({}, d1), d2)
    assert index_get(index, 1, "name") == "Alice"
    assert index_get(index, 1, "age") == 30


def test_remove_datom_leaves_other_attrs():
    d1 = Datom(entity_id=1, attr_name="name", value="Alice")
    d2 = Datom(entity_id=1, attr_name="age", value=30)
    index = index_add(index_add({}, d1), d2)
    result = index_remove(index, d1)
    assert index_get(result, 1, "name") is None
    assert index_get(result, 1, "age") == 30


def test_index_get_returns_copy_not_reference():
    d = Datom(entity_id=1, attr_name="name", value="Alice")
    index = index_add({}, d)
    attrs = index_get(index, 1)
    attrs["hacked"] = "evil"
    assert index_get(index, 1) == {"name": "Alice"}


def test_index_add_does_not_mutate_original():
    d = Datom(entity_id=1, attr_name="name", value="Alice")
    original = {}
    index_add(original, d)
    assert original == {}


def test_add_entity_to_layer_updates_storage_and_eavt():
    name_attr = Attribute(name="name", value="Alice", type=":db/string", cardinality=":db/single")
    entity = Entity(id=1, attrs={"name": name_attr})
    layer = Layer()
    new_layer = add_entity_to_layer(layer, entity)
    assert new_layer.storage[1] is entity
    assert index_get(new_layer.eavt, 1, "name") == "Alice"
