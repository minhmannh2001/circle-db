from constructs import make_db, Entity, Attribute
from db import add_entity
from graph import outgoing_refs, incoming_refs


def make_attr(value, type=":db/string", cardinality=":db/single"):
    return Attribute(name="", value=value, type=type, cardinality=cardinality)


def make_ref(target_id):
    return Attribute(name="", value=target_id, type=":db/ref", cardinality=":db/single")


# --- outgoing_refs ---

def test_outgoing_refs_returns_referenced_entity_ids():
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={}))                       # id=1
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"friend": make_ref(1)}))  # id=2, refs 1
    assert outgoing_refs(db, 2) == [1]


def test_outgoing_refs_empty_when_no_refs():
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice")}))
    assert outgoing_refs(db, 1) == []


# --- incoming_refs ---

def test_incoming_refs_returns_referencing_entity_ids():
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={}))                       # id=1
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"friend": make_ref(1)}))  # id=2, refs 1
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"friend": make_ref(1)}))  # id=3, refs 1
    assert set(incoming_refs(db, 1)) == {2, 3}


def test_incoming_refs_empty_when_not_referenced():
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice")}))
    assert incoming_refs(db, 1) == []
