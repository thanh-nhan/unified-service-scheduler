# ADR-002: Database-enforced overlap prevention

**Status:** Proposed · **Date:** 2026-10-08

## Context

The core requirement is that a confirmed appointment has a technician and a bay for its whole duration. Requests arrive concurrently. The naive "check availability, then insert" approach lets two requests both pass the check and both insert, double-booking a resource. AI coding tools typically generate exactly this naive version.

## Decision

Use **PostgreSQL exclusion constraints** on `appointment`, one each for technician, bay, and vehicle:

```sql
EXCLUDE USING gist (technician_id WITH =, time_range WITH &&) WHERE (status = 'CONFIRMED')
```

Application queries choose good candidates. The database guarantees correctness. A lost race raises `SQLSTATE 23P01`, and the application retries in a new transaction, up to 3 attempts (04 §4).

## Consequences

- Double-booking is impossible regardless of application bugs, new endpoints, or the number of API instances.
- Concurrent bookings for different resources never block each other. Only inserts that truly conflict wait for each other.
- Requires PostgreSQL (`btree_gist`, range types). Tests must run on real PostgreSQL (Testcontainers), never H2.
- The retry loop must sit outside the transaction (a known pitfall, called out for the coding model).

## Alternatives considered

| Option | Assessment |
|---|---|
| `SELECT … FOR UPDATE` on technician and bay rows, then re-check and insert | Works, but correctness depends on every write path following the same locking protocol and lock order (deadlock risk). Serializes all bookings per resource |
| PostgreSQL advisory locks per resource | Same weakness: a convention in code, not a rule in the database |
| SERIALIZABLE isolation | Correct, but serialization failures can occur for unrelated bookings, requiring retries of everything; harder to reason about |
| MySQL with row locking | No exclusion constraints. Must use READ COMMITTED for the re-check, because under MySQL's default REPEATABLE READ the re-check reads a stale snapshot and double-books even after waiting for the lock. Lock ordering needed |
| MySQL with a time-slot table and a primary key on (resource, slot) | Database-enforced, but only for grid-aligned times; adds two tables and many rows per appointment |
