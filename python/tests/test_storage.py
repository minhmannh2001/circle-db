from constructs import Entity
from storage import add_entity, get_entity, remove_entity


def test_add_entity_then_get_it_back():
    e = Entity(id=1)
    storage = add_entity({}, e)
    assert get_entity(storage, 1) is e


def test_add_entity_does_not_mutate_original():
    original = {}
    add_entity(original, Entity(id=1))
    assert original == {}


def test_get_entity_returns_none_for_missing_id():
    assert get_entity({}, 99) is None


def test_remove_entity_removes_correct_entity():
    e1, e2 = Entity(id=1), Entity(id=2)
    storage = add_entity(add_entity({}, e1), e2)
    result = remove_entity(storage, 1)
    assert get_entity(result, 1) is None
    assert get_entity(result, 2) is e2


def test_remove_entity_does_not_mutate_original():
    e = Entity(id=1)
    original = add_entity({}, e)
    remove_entity(original, 1)
    assert get_entity(original, 1) is e
