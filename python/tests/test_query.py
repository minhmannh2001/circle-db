from constructs import Datom, make_db, Entity, Attribute
from db import add_entity
from query import transform, plan, execute, unify, q


def make_attr(value, type=":db/string", cardinality=":db/single"):
    return Attribute(name="", value=value, type=type, cardinality=cardinality)


# --- transform ---

def test_transform_matches_datom_where_constants_equal():
    predicate = transform(["?e", "name", "Alice"])
    datom = Datom(entity_id=1, attr_name="name", value="Alice")
    assert predicate(datom) is True


def test_transform_rejects_datom_where_value_differs():
    predicate = transform(["?e", "name", "Alice"])
    datom = Datom(entity_id=2, attr_name="name", value="Bob")
    assert predicate(datom) is False


def test_transform_variable_slots_are_wildcards():
    predicate = transform(["?e", "?a", "Alice"])
    assert predicate(Datom(entity_id=1, attr_name="name", value="Alice")) is True
    assert predicate(Datom(entity_id=99, attr_name="age", value="Alice")) is True
    assert predicate(Datom(entity_id=1, attr_name="name", value="Bob")) is False


# --- plan ---

def _db_with_alice():
    db = make_db()
    return add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice")}))


def test_plan_entity_unknown_queries_avet():
    db = _db_with_alice()
    plan_fn = plan([["?e", "name", "Alice"]], db.layers[-1])
    result = plan_fn()
    assert 1 in result


def test_plan_entity_unknown_returns_all_matching_entities():
    # Two entities both named "Alice" — AVET should return both
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice")}))
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Bob")}))
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice")}))
    plan_fn = plan([["?e", "name", "Alice"]], db.layers[-1])
    result = plan_fn()
    assert result == {1, 3}


def test_plan_value_unknown_queries_eavt():
    db = _db_with_alice()
    plan_fn = plan([[1, "name", "?v"]], db.layers[-1])
    result = plan_fn()
    assert result == "Alice"


def test_plan_attr_unknown_queries_veat():
    db = _db_with_alice()
    plan_fn = plan([[1, "?a", "Alice"]], db.layers[-1])
    result = plan_fn()
    assert "name" in result


def test_plan_attr_unknown_returns_all_matching_attrs():
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={
        "name":     make_attr("Alice"),
        "username": make_attr("Alice"),
    }))
    plan_fn = plan([[1, "?a", "Alice"]], db.layers[-1])
    result = plan_fn()
    assert result == {"name", "username"}


# --- range scans (2 variables) ---

def _db_with_alice_and_bob():
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice"), "age": make_attr(30)}))
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Bob"),   "age": make_attr(25)}))
    return db


def test_plan_attr_known_scans_avet_range():
    # ["?e", "name", "?v"] — only attr known → all values under avet["name"]
    db = _db_with_alice_and_bob()
    plan_fn = plan([["?e", "name", "?v"]], db.layers[-1])
    result = plan_fn()
    assert result == {"Alice": {1}, "Bob": {2}}


def test_plan_entity_known_scans_eavt_range():
    # [1, "?a", "?v"] — only entity known → all attr/value pairs for entity 1
    db = _db_with_alice_and_bob()
    plan_fn = plan([[1, "?a", "?v"]], db.layers[-1])
    result = plan_fn()
    assert result == {"name": "Alice", "age": 30}


def test_plan_value_known_scans_veat_range():
    # ["?e", "?a", "Alice"] — only value known → all entity/attr pairs with value Alice
    db = _db_with_alice_and_bob()
    plan_fn = plan([["?e", "?a", "Alice"]], db.layers[-1])
    result = plan_fn()
    assert result == {1: {"name"}}


# --- execute ---

def test_execute_entity_unknown_returns_bindings():
    db = _db_with_alice()
    layer = db.layers[-1]
    result = execute([["?e", "name", "Alice"]], layer)
    assert result == [{"?e": 1}]


def test_execute_value_variable_bound_in_range_scan():
    db = _db_with_alice()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Bob")}))
    layer = db.layers[-1]
    result = execute([["?e", "name", "?name"]], layer)
    assert {"?e": 1, "?name": "Alice"} in result
    assert {"?e": 2, "?name": "Bob"} in result
    assert len(result) == 2


def test_execute_two_clause_and_intersects_entity_sets():
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice"), "age": make_attr(30)}))
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Bob"),   "age": make_attr(25)}))
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice"), "age": make_attr(25)}))
    layer = db.layers[-1]
    result = execute([["?e", "name", "Alice"], ["?e", "age", 30]], layer)
    assert result == [{"?e": 1}]


# --- unify ---

def test_unify_projects_single_find_var():
    bindings = [{"?e": 1, "?name": "Alice"}, {"?e": 2, "?name": "Bob"}]
    assert sorted(unify(bindings, ["?e"])) == [[1], [2]]


def test_unify_projects_multiple_find_vars_in_order():
    bindings = [{"?e": 1, "?name": "Alice"}, {"?e": 2, "?name": "Bob"}]
    assert sorted(unify(bindings, ["?name", "?e"])) == [["Alice", 1], ["Bob", 2]]


# --- q ---

def test_q_single_clause_returns_matching_entities():
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice")}))
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Bob")}))
    assert q({"find": ["?e"], "where": [["?e", "name", "Alice"]]}, db) == [[1]]


def test_q_two_clause_where_returns_only_entities_matching_all_clauses():
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice"), "age": make_attr(30)}))
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Bob"),   "age": make_attr(25)}))
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice"), "age": make_attr(25)}))
    assert q({"find": ["?e"], "where": [["?e", "name", "Alice"], ["?e", "age", 30]]}, db) == [[1]]


def test_q_historical_layer_returns_old_state():
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice")}))
    historical_db = db
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Bob")}))
    assert len(q({"find": ["?e", "?name"], "where": [["?e", "name", "?name"]]}, db)) == 2
    assert q({"find": ["?e", "?name"], "where": [["?e", "name", "?name"]]}, historical_db) == [[1, "Alice"]]


def test_q_attr_unknown_returns_entity_and_attr():
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice"), "age": make_attr(30)}))
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Bob")}))
    assert q({"find": ["?e", "?a"], "where": [["?e", "?a", "Alice"]]}, db) == [[1, "name"]]


def test_q_attr_unknown_expands_multiple_attrs_per_entity():
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={
        "name":     make_attr("Alice"),
        "nickname": make_attr("Alice"),
    }))
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Bob")}))
    assert sorted(q({"find": ["?e", "?a"], "where": [["?e", "?a", "Alice"]]}, db)) == [[1, "name"], [1, "nickname"]]


def test_q_attr_unknown_combined_with_other_clause():
    db = make_db()
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice"), "age": make_attr(30)}))
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Bob"),   "age": make_attr(25)}))
    db = add_entity(db, Entity(id=":db/no-id-yet", attrs={"name": make_attr("Alice"), "age": make_attr(25)}))
    assert q({"find": ["?e", "?a"], "where": [["?e", "?a", "Alice"], ["?e", "age", 30]]}, db) == [[1, "name"]]
