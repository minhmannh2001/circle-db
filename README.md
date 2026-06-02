# circle-db

A functional, immutable, in-memory database built from scratch — twice. Once in Python, once in Clojure.

Inspired by the [functionalDB chapter](https://aosabook.org/en/500L/an-archaeological-dig-redux.html) from *500 Lines or Less*. The goal is not to produce a production database, but to understand how one works by building it phase by phase.

## What it is

circle-db stores data as **datoms** — `(entity, attribute, value)` triples — and never overwrites anything. Every update appends a new layer of history, making time-travel queries a first-class feature.

## Structure

```
python/     Python implementation
clojure/    Clojure implementation
```
