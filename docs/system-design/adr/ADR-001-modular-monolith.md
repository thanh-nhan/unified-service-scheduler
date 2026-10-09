# ADR-001: Modular monolith

**Status:** Proposed · **Date:** 2026-10-08

## Context

The MVP has one team, one database, and one strongly consistent core operation (booking) that spans resources, customers, and appointments. Load is modest (01, A-01 and A-02).

## Decision

Build one Spring Boot service with feature modules (`catalog`, `customer`, `availability`, `booking`, `common`) and enforced dependency rules (02 §4). One PostgreSQL database.

## Consequences

- Booking stays a single local transaction; no distributed transactions or sagas.
- One artifact to build, test, deploy, and observe.
- Module boundaries keep the code ready to split later (for example, availability reads into a separate read service) if scale or team structure requires it.
- The whole API scales as one unit, which is acceptable because it is stateless and horizontally scalable (09 §3).

## Alternatives considered

| Option | Why not |
|---|---|
| Microservices (scheduling, catalog, customer) | Network calls and distributed consistency for the core booking operation, with no benefit at this scale |
| Single-package monolith | Faster to start, but boundaries erode and a local coding model gets less structure to follow |
