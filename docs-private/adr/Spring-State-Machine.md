# Spring State Machine


## Status

**Accepted**


## Context

The project started with a custom implementation, where transitions logic was programmed imperatively.
The states were persisted in the database in Identity Verification entity as `phase` and `status`.


## Decision

The decision was made to migrate to Spring State Machine library to simplify the state management and transitions logic and to prefer a declarative approach.


## Consequences

Unfortunately, it happened at the late stage of the project with not enough time to refactor the code.
So the state is still represented in the database as a combination of two columns.
Moreover, the state is not persisted by the library, although it supports that feature thanks to `JpaStateMachineRepository`.

Currently, the state is changed and persisted in action beans, which may look like an unnecessary side effect.
