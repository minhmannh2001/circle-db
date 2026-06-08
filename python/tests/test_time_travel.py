from constructs import make_db, Entity, Attribute
from db import add_entity, update_entity
from time_travel import advance_time, evolution_of, db_at
from query import q


def make_attr(value, type=":db/string", cardinality=":db/single"):
    return Attribute(name="", value=value, type=type, cardinality=cardinality)


# --- advance_time ---

def test_advance_time_increments_curr_time():
    db = make_db()
    db2 = advance_time(db)
    assert db2.curr_time == db.curr_time + 1


# --- evolution_of ---

def test_evolution_of_single_update_returns_one_entry():
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice")}))
    db = advance_time(db)   # curr_time=1
    db = update_entity(db, 1, "name", "Bob")
    assert evolution_of(db, 1, "name") == [(1, "Bob")]


def test_evolution_of_three_updates_returns_all_in_order():
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice")}))
    db = advance_time(db)
    db = update_entity(db, 1, "name", "Bob")
    db = advance_time(db)
    db = update_entity(db, 1, "name", "Charlie")
    db = advance_time(db)
    db = update_entity(db, 1, "name", "Dave")
    assert evolution_of(db, 1, "name") == [(1, "Bob"), (2, "Charlie"), (3, "Dave")]


def test_evolution_of_no_updates_returns_empty():
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice")}))
    assert evolution_of(db, 1, "name") == []


# --- db_at ---

def test_db_at_query_reflects_state_at_timestamp():
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice")}))
    db = advance_time(db)                          # curr_time=1
    db = update_entity(db, 1, "name", "Bob")      # ts=1
    db = advance_time(db)                          # curr_time=2
    db = update_entity(db, 1, "name", "Charlie")  # ts=2

    # At t=1 the name was "Bob"
    assert q({"find": ["?e", "?v"], "where": [["?e", "name", "?v"]]}, db_at(db, 1)) == [[1, "Bob"]]
    # Current state is "Charlie"
    assert q({"find": ["?e", "?v"], "where": [["?e", "name", "?v"]]}, db) == [[1, "Charlie"]]
