from dataclasses import dataclass, field
from typing import Any


@dataclass
class Datom:
    entity_id: int
    attr_name: str
    value: Any


@dataclass
class Database:
    layers: list
    top_id: int
    curr_time: int


def make_db() -> "Database":
    return Database(layers=[Layer()], top_id=0, curr_time=0)


@dataclass
class Layer:
    storage: dict = field(default_factory=dict)
    eavt: dict = field(default_factory=dict)
    avet: dict = field(default_factory=dict)
    veat: dict = field(default_factory=dict)
    vaet: dict = field(default_factory=dict)


@dataclass
class Entity:
    id: int | str  # str used as sentinel ":db/no-id-yet" before ID is assigned
    attrs: dict = field(default_factory=dict)


@dataclass
class Attribute:
    name: str
    value: Any
    type: str
    cardinality: str
    ts: int = -1
    prev_ts: int = -1
