# 01: Context and scope

## Problem

Dealerships book service appointments manually (phone, spreadsheets). Staff must check by hand that a qualified technician and a suitable service bay are both free, which is slow and leads to double-bookings. The Unified Service Scheduler lets vehicle owners book online and guarantees that a confirmed appointment always has a technician and a bay reserved for its whole duration.

## Actors

| Actor | Interaction |
|---|---|
| Vehicle owner (customer) | Browses services, views availability, books, views, cancels, and lists appointments through a client app (out of scope; mocked by Swagger UI and a curl script) |
| Dealership staff | Not a user of the MVP. Technicians, bays, and opening hours are seed data |
| Operations team | Monitors the service through logs, metrics, traces, and health endpoints |

## Scope

**In scope (MVP):** everything in user stories v0.3. That is reference data, daily and weekly availability, booking with automatic technician and bay assignment, validation, concurrency safety, idempotency, viewing, cancelling, listing, observability, and API documentation.

**Out of scope:** see the [user stories README](../user-stories/README.md#out-of-scope-mvp). The design notes where each out-of-scope feature would plug in (09 §6).

## Assumptions

The user stories' conventions (`00-conventions.md`) define the business rules. These are the additional **technical** assumptions this design makes:

| ID | Assumption |
|---|---|
| A-01 | One logical database for all dealerships. Data volume is small: about 200 dealerships, 10 technicians and 8 bays each, about 20 appointments per dealership per day (about 1.5 million appointments per year). |
| A-02 | Peak load is about 10 booking requests per second nationally, and about 50 times more availability reads than bookings. |
| A-03 | The client is trusted to send the correct `customerId` (no authentication in the MVP). Production would take it from a verified JWT. |
| A-04 | The service and its users are all in Vietnam (UTC+07:00, no daylight saving). See ADR-003. |
| A-05 | Clients may retry requests on network failure, so booking must be idempotent. |
| A-06 | Technicians work whenever the dealership is open. Shifts, breaks, and leave are out of scope. |

## Quality targets

| Attribute | Target | How it is met |
|---|---|---|
| Correctness | **Zero** overlapping CONFIRMED appointments for the same technician, bay, or vehicle, under any concurrency | Database exclusion constraints (ADR-002), concurrency tests (AC-05.1, AC-05.2) |
| Latency | p95 below 300 ms for booking and weekly availability at peak load, on one application instance | Indexed queries, a fixed number of queries per request (04, 05) |
| Availability | 99.9% monthly for the API | Stateless instances behind a load balancer, managed PostgreSQL with failover (09) |
| Recoverability | No confirmed booking lost after a 201 response | Commit before responding; database backups with point-in-time recovery (09) |
| Observability | Every request traceable by `X-Request-Id`; booking outcomes and conflicts measurable | 08 |
| Maintainability | New feature modules can be added without touching booking internals; every AC has an automated test | 02 (module rules), Definition of Done in stories |
