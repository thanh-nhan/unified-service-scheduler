# 09: Scalability, reliability, and security

## 1. Load profile

Assumptions A-01 and A-02 (01): about 4,000 bookings a day nationally, peaks of about 10 bookings and 500 availability reads per second. One API instance and one modest PostgreSQL instance can serve this. The design keeps a clear path to grow well beyond it.

## 2. Performance

| Technique | Effect |
|---|---|
| Fixed number of queries per request (availability: 4; booking: about 8) | Latency does not grow with the number of slots, days, or resources |
| GiST indexes from the exclusion constraints | Overlap checks are index lookups, not table scans |
| B-tree indexes on `(dealership_id, start_time)` and `(customer_id, start_time, id)` | Availability ranges and customer lists are index range scans |
| Pure in-memory slot calculation | Microseconds per week of availability |
| Virtual threads and a bounded connection pool (HikariCP) | Many concurrent requests waiting on I/O without exhausting threads; the pool protects the database |

## 3. Scaling path

| Stage | Trigger | Change |
|---|---|---|
| 1 (MVP) | n/a | One stateless API instance, one PostgreSQL instance |
| 2 | Availability or failover needs | 2+ API instances behind a load balancer (the API holds no state; idempotency and correctness live in the database). Managed PostgreSQL with a synchronous standby |
| 3 | Read load grows | Serve availability from a **read replica**. Optionally cache weekly availability per (dealership, service type, week) for a few seconds, evicted by booking and cancel events. Bookings always go to the primary, so correctness does not depend on cache freshness |
| 4 | Write volume or data size grows | Partition `appointment` by dealership or by month. Every query is already scoped by dealership or resource. PostgreSQL requires a partitioned table's exclusion constraints to include the partition key, so each constraint gains `dealership_id WITH =`; this changes nothing logically because a technician, bay, or vehicle booking always belongs to one dealership |
| 5 | Multiple regions or very large scale | Shard by dealership (a dealership's resources and appointments always live together). Split modules into services only if teams or scaling needs diverge (ADR-001) |

## 4. Reliability

| Failure | Behavior |
|---|---|
| Two customers race for the same technician or bay | One wins; the other retries with fresh data or receives 409 (04 §4). Never a double-booking |
| Client times out and retries | Same `Idempotency-Key` returns the original appointment (ADR-004) |
| API instance crashes mid-booking | The transaction rolls back; nothing is half-written (G2). The client retries with the same key |
| Database unavailable | Readiness fails, so instances leave the load balancer. Requests return 500 with a `requestId`. No partial writes |
| Slow database | Connection pool and statement timeouts bound waiting; requests fail fast instead of piling up |
| Deployment | Graceful shutdown (`server.shutdown=graceful`) finishes in-flight requests. Flyway migrations are backward-compatible (expand, then contract) so old and new instances can run side by side |
| Data loss | Managed PostgreSQL backups with point-in-time recovery. A 201 is only sent after commit |

## 5. Security (production design; MVP is unauthenticated)

| Concern | Design |
|---|---|
| Authentication | OAuth 2.0 / OIDC access tokens (JWT) validated by Spring Security resource server. `customerId` comes from the token subject, not the request body |
| Authorization | A customer may read, cancel, and list only their own appointments. Dealership staff roles would be scoped to their dealership |
| Input validation | Strict parsing, Bean Validation, length limits on headers (`Idempotency-Key`, `X-Request-Id`) |
| Data protection | TLS everywhere. No PII in logs, metrics, or traces (GR-12). Responses exclude email and phone |
| Abuse | Rate limiting per customer and IP at the gateway, to protect booking and availability endpoints |
| Secrets | Database credentials from environment or a secret manager, never in the repository |
| SQL injection | Parameterized queries only (JPA and `JdbcClient` named parameters) |

## 6. Where out-of-scope features plug in

| Feature | Plug-in point |
|---|---|
| Notifications (SMS, email) | Transactional outbox: insert an `AppointmentConfirmed` or `AppointmentCancelled` event row in the same transaction as the booking or cancel; a relay publishes it to a message broker |
| Rescheduling | A new action endpoint that cancels and books in one transaction, reusing the attempt logic |
| Technician shifts, breaks, leave | A `technician_unavailability` table; selection queries and the calculator treat those intervals as busy |
| Public holidays | A `dealership_closure` table checked alongside opening hours |
| Multiple services per appointment | Sum durations, require the union of skills (or a team); remains one interval per appointment |
| Multiple time zones | Store `timestamptz` and a dealership time zone (the original design option, see ADR-003) |
| Idempotency key retention | Keys are stored with appointments. A separate table with a time-to-live would be needed only if failed requests had to be remembered too |
