from constructs import make_db, Entity, Attribute
from db import add_entity
from transactions import transact, what_if


def make_attr(value, type=":db/string", cardinality=":db/single"):
    return Attribute(name="", value=value, type=type, cardinality=cardinality)


def add_alice(db):
    return add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice")}))


def add_bob(db):
    return add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Bob")}))


# --- transact ---

def test_transact_two_ops_adds_exactly_one_layer():
    db = make_db()
    new_db = transact(db, [add_alice, add_bob])
    assert len(new_db.layers) == len(db.layers) + 1


def test_transact_all_entities_visible_in_result():
    db = make_db()
    new_db = transact(db, [add_alice, add_bob])
    layer = new_db.layers[-1]
    assert layer.storage[1].attrs["name"].value == "Alice"
    assert layer.storage[2].attrs["name"].value == "Bob"


def test_transact_original_db_unchanged():
    db = make_db()
    original_layer_count = len(db.layers)
    transact(db, [add_alice, add_bob])
    assert len(db.layers) == original_layer_count


# --- what_if ---

def test_what_if_result_has_changes():
    db = make_db()
    result_db = what_if(db, [add_alice, add_bob])
    layer = result_db.layers[-1]
    assert layer.storage[1].attrs["name"].value == "Alice"
    assert layer.storage[2].attrs["name"].value == "Bob"


def test_what_if_original_db_unchanged():
    db = make_db()
    original_layer_count = len(db.layers)
    what_if(db, [add_alice, add_bob])
    assert len(db.layers) == original_layer_count


def test_transact_historical_layer_has_pre_transaction_state():
    db = make_db()
    db = transact(db, [add_alice])
    db = transact(db, [add_bob])
    # The layer before the last one has the state after the first transact
    pre_last = db.layers[-2]
    assert 1 in pre_last.storage
    assert 2 not in pre_last.storage
