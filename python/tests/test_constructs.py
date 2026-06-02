from constructs import Datom, Attribute, Entity, Layer, Database, make_db


def test_datom_holds_three_fields():
    d = Datom(entity_id=1, attr_name="name", value="Alice")
    assert d.entity_id == 1
    assert d.attr_name == "name"
    assert d.value == "Alice"


def test_make_db_returns_empty_database():
    db = make_db()
    assert isinstance(db, Database)
    assert len(db.layers) == 1
    assert isinstance(db.layers[0], Layer)
    assert db.top_id == 0
    assert db.curr_time == 0


def test_database_nests_layers():
    db = Database(layers=[Layer()], top_id=0, curr_time=0)
    assert isinstance(db.layers[0], Layer)
    assert db.layers[0].eavt == {}
    assert db.top_id == 0
    assert db.curr_time == 0


def test_layer_has_storage_and_four_empty_indexes():
    layer = Layer()
    assert layer.storage == {}
    assert layer.eavt == {}
    assert layer.avet == {}
    assert layer.veat == {}
    assert layer.vaet == {}


def test_entity_has_id_and_empty_attrs():
    e = Entity(id=1)
    assert e.id == 1
    assert e.attrs == {}


def test_attribute_fields_and_defaults():
    a = Attribute(name="name", value="Alice", type=":db/string", cardinality=":db/single")
    assert a.name == "name"
    assert a.value == "Alice"
    assert a.type == ":db/string"
    assert a.cardinality == ":db/single"
    assert a.ts == -1
    assert a.prev_ts == -1
