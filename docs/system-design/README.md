# System Design: Unified Service Scheduler

**Status:** APPROVED · **Version:** 0.2 · **Date:** 2026-10-09
**Requirements baseline:** [User stories v0.4 (approved)](../user-stories/README.md)

This folder is the System Design Document for Scenario A. It explains **how** the approved user stories are built and **why**. The user stories remain the source of truth for **what** the system does. If a design document and a story disagree, the story wins and the design is corrected.

## Document map

| # | Document | Answers |
|---|---|---|
| 01 | [Context and scope](01-context-and-scope.md) | Problem, actors, scope, assumptions, quality targets |
| 02 | [Architecture](02-architecture.md) | **Architecture diagram**, component roles, package structure, **data flow** |
| 03 | [Data model](03-data-model.md) | Tables, constraints, indexes, migrations, fixture data |
| 04 | [Booking and concurrency](04-booking-and-concurrency.md) | How a booking is validated, allocated, and protected against double-booking; idempotency; cancellation |
| 05 | [Availability](05-availability.md) | Daily and weekly slot calculation |
| 06 | [API](06-api.md) | Endpoints, conventions, error model |
| 07 | [Technology choices](07-technology-choices.md) | **Technologies with justifications** |
| 08 | [Observability](08-observability.md) | **Logging, metrics, tracing, health** |
| 09 | [Scalability and reliability](09-scalability-and-reliability.md) | Performance, scaling path, failure modes, security |
| 10 | [GenAI in the design phase](10-genai-in-design.md) | **How GenAI was used to produce this design** |
| ADR | [Architecture decision records](adr/) | The key decisions, with alternatives considered |

## Key decisions at a glance

| Decision | Summary | Record |
|---|---|---|
| Modular monolith | One Spring Boot service with feature modules, one PostgreSQL database | [ADR-001](adr/ADR-001-modular-monolith.md) |
| Database-enforced overlap prevention | PostgreSQL exclusion constraints make double-booking impossible, even under concurrency | [ADR-002](adr/ADR-002-database-enforced-overlap.md) |
| Single time zone | All times are Vietnam local time, stored as `timestamp` without time zone | [ADR-003](adr/ADR-003-single-time-zone.md) |
| Idempotent booking | `Idempotency-Key` stored on the appointment with a request hash | [ADR-004](adr/ADR-004-idempotency.md) |
| Spring Boot 4.1 | Spring Boot 3.5 open-source support ended on 2026-06-30 | [ADR-005](adr/ADR-005-spring-boot-4.md) |
| No alternative-slot suggestions | On conflict the API returns 409 only; the UI re-reads availability | [ADR-006](adr/ADR-006-no-alternative-suggestions.md) |

## Traceability: stories to design

| Story | Primary design sections |
|---|---|
| E-01 Foundation | 02 (packages), 03 (migrations, fixture), 07 |
| US-01 Reference data | 02, 06 |
| US-02 Daily availability | 05 |
| US-09 Weekly availability | 05 |
| US-03 Book | 04 §2–§4 |
| US-04 Validation | 04 §2 |
| US-05 Concurrency and idempotency | 04 §4–§5, ADR-002, ADR-004 |
| US-06 View | 06, 04 §7 |
| US-07 Cancel | 04 §6 |
| US-08 List my appointments | 06, 03 (indexes) |
| E-02 Observability | 08 |
| E-03 API documentation | 06, 07 |

## Change log

| Version | Date | Change | Approved by |
|---|---|---|---|
| 0.1 | 2026-10-08 | Initial draft based on user stories v0.3 | EM, 2026-10-09 |
| 0.2 | 2026-10-09 | Build tool changed from Maven to Gradle (Kotlin DSL, version catalog): 02, 07 | EM, 2026-10-09 |
