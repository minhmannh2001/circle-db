# circle-db

A functional, immutable, in-memory database built from scratch — twice. Once in Python, once in Clojure.

Inspired by the [functionalDB chapter](https://aosabook.org/en/500L/an-archaeological-dig-redux.html) from *500 Lines or Less*.

## What it does

- Stores data as **EAV datoms** — `(entity, attribute, value)` triples
- Maintains 4 indexes (EAVT, AVET, VEAT, VAET) so every query is an index lookup, never a full scan
- Appends immutable layers on each write — no data is ever overwritten
- Supports atomic transactions and speculative "what-if" queries
- Queries via a Datalog-style `q` function with unification across clauses
- Time-travels to any past timestamp, or walks the full history of any attribute
- Traverses entity references as a bidirectional graph via the VAET index

## Phases

| # | Concept | Blog |
|---|---------|------|
| 0 | Tooling & REPL workflow | — |
| 1 | EAV data model | [post](https://minhmannh2001.github.io/2026/06/02/circle-db-phase-01-data-model-en.html) |
| 2 | In-memory storage | [post](https://minhmannh2001.github.io/2026/06/02/circle-db-phase-02-in-memory-storage-en.html) |
| 3 | EAVT index | [post](https://minhmannh2001.github.io/2026/06/02/circle-db-phase-03-eavt-index-en.html) |
| 4 | All 4 indexes | [post](https://minhmannh2001.github.io/2026/06/02/circle-db-phase-04-four-indexes-en.html) |
| 5 | CRUD write path | [post](https://minhmannh2001.github.io/2026/06/04/circle-db-phase-05-crud-write-path-en.html) |
| 6 | Transactions + speculative queries | [post](https://minhmannh2001.github.io/2026/06/04/circle-db-phase-06-transactions-en.html) |
| 7 | Query planning | [post](https://minhmannh2001.github.io/2026/06/05/circle-db-phase-07-query-planning-en.html) |
| 8 | Query execution & unification | [post](https://minhmannh2001.github.io/2026/06/08/circle-db-phase-08-query-execution-en.html) |
| 9 | Time-travel + graph traversal | [post](https://minhmannh2001.github.io/2026/06/08/circle-db-phase-09-time-travel-graph-en.html) |

## Usage

```python
from constructs import make_db, Entity, Attribute
from db import add_entity, update_entity
from transactions import transact, what_if
from query import q
from time_travel import evolution_of, db_at

def attr(value, type=":db/string", cardinality=":db/single"):
    return Attribute(name="", value=value, type=type, cardinality=cardinality)

db = make_db()

# Add entities
db = transact(db, [
    lambda d: add_entity(d, Entity(":db/no-id-yet", {"name": attr("Alice"), "age": attr(30, ":db/long")})),
    lambda d: add_entity(d, Entity(":db/no-id-yet", {"name": attr("Bob"),   "age": attr(25, ":db/long")})),
])

# Query — find all names
q({"find": ["?name"], "where": [["?e", "name", "?name"]]}, db)
# => [{"?name": "Alice"}, {"?name": "Bob"}]

# Query — find entity by known name
q({"find": ["?e"], "where": [["?e", "name", "Alice"]]}, db)
# => [{"?e": 1}]

# Update and track history
db = transact(db, [lambda d: update_entity(d, 1, "age", 31)])
db = transact(db, [lambda d: update_entity(d, 1, "age", 32)])

evolution_of(db, 1, "age")   # => [(0, 30), (1, 31), (2, 32)]

# Time-travel — see the database as it was at timestamp 1
old_db = db_at(db, 1)
q({"find": ["?age"], "where": [[1, "age", "?age"]]}, old_db)
# => [{"?age": 31}]

# Speculative query — try a change without committing
draft = what_if(db, [lambda d: update_entity(d, 2, "age", 99)])
q({"find": ["?age"], "where": [[2, "age", "?age"]]}, draft)   # => 99
q({"find": ["?age"], "where": [[2, "age", "?age"]]}, db)      # => 25 (unchanged)
```

## Structure

```
python/     Python implementation
clojure/    Clojure implementation
journal/    One markdown file per phase — concept, decisions, insights
```

## Run tests

```bash
# Python
pytest python/tests/

# Clojure
cd clojure && clj -M:test
```
