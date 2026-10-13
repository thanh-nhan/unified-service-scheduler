# 08: Observability

Goal: an operator can answer three questions quickly. **Is it up?** (health). **Is it healthy?** (metrics and alerts). **What happened to this request?** (logs and traces, joined by `requestId` and `traceId`).

## 1. Correlation

| Identifier | Source | Where it appears |
|---|---|---|
| `requestId` | `X-Request-Id` header from the client, or a generated UUID. Accepted only if 1–64 characters of `[A-Za-z0-9._-]` (prevents log injection); otherwise a new one is generated | Response header, every log line (MDC), every problem response |
| `traceId`, `spanId` | OpenTelemetry (Micrometer Tracing) | Every log line, trace backend |

A client or support agent can quote a `requestId` from an error response, and an operator can find every log line and the trace for that request.

## 2. Logging

- **Format:** one JSON object per line (Spring Boot structured logging, Logstash format), written to stdout. The container platform ships stdout to the log store. Local runs without the `dev` profile may use plain text for readability.
- **Standard fields:** `@timestamp`, `level`, `logger_name`, `message`, `requestId`, `traceId`, `spanId`, `thread_name`.
- **Privacy (GR-12):** logs contain IDs only, never names, emails, phone numbers, or VINs.

| Event | Level | Fields |
|---|---|---|
| `booking.completed` (exactly one per booking request, AC-E02.4) | INFO | `outcome` (`CONFIRMED` or error code), `appointmentId` (on success), `dealershipId`, `serviceTypeCode`, `attempts`, `durationMs`, `replayed` |
| `booking.conflict.retry` | INFO | `constraint`, `attempt` |
| `appointment.cancelled` | INFO | `appointmentId`, `dealershipId` |
| Validation and business rejections | none beyond `booking.completed` | (they are expected outcomes, not errors) |
| Unexpected exception | ERROR | stack trace, `requestId` |

Booking events use the SLF4J fluent API (`log.atInfo().addKeyValue(...)`), so fields arrive as JSON properties rather than text to parse.

## 3. Metrics

Exposed at `/actuator/prometheus`.

| Metric (Micrometer name) | Type | Tags | Purpose |
|---|---|---|---|
| `scheduler.booking.attempts` | Counter | `outcome` | Booking outcomes: CONFIRMED vs each rejection code |
| `scheduler.booking.duration` | Timer with histogram | `outcome` | Booking latency, p50/p95/p99 |
| `scheduler.booking.conflict.retries` | Counter | `constraint` (technician, bay, or `deadlock`) | How often concurrent bookings collide |
| `scheduler.availability.duration` | Timer with histogram | `range` (`day`/`week`) | Availability latency |
| `http.server.requests` | Timer (built in) | `uri`, `method`, `status` | Request rate, errors, latency per endpoint |
| `hikaricp.connections.*` | Gauges (built in) | n/a | Connection pool saturation |
| `jvm.*`, `process.*` | Built in | n/a | Memory, GC, CPU, threads |

`outcome` and `constraint` have small fixed value sets, so tags stay low-cardinality. IDs are never used as tags.

### What the business metrics mean

- **Capacity rejection rate** (`NO_QUALIFIED_TECHNICIAN_AVAILABLE` + `NO_SERVICE_BAY_AVAILABLE` over all attempts, broken down by dealership in the log store): demand the dealership cannot serve. A sustained high rate means the dealership needs more technicians, more bays, or longer opening hours. This is a sales and operations signal, not just a technical one.
- **Conflict retry rate**: how often customers race for the same resources. Rising retries with stable traffic indicate popular time slots.
- **`HIGH_CONTENTION` count**: should be near zero. Any sustained value means the retry bound or the allocation strategy needs attention.

## 4. Tracing

- Spring MVC, JDBC, and outgoing calls are traced through Micrometer Tracing with the OpenTelemetry bridge.
- Booking adds custom spans: `booking.attempt` (with the attempt number) and `booking.allocate`.
- Sampling: 100% locally; 10% plus all errors in production (configurable).
- The OTLP exporter is **off by default** and enabled by environment variable, so the service runs without a collector. A Compose profile with an OpenTelemetry collector and Jaeger is an optional extra.

## 5. Health

| Endpoint | Includes | Used by |
|---|---|---|
| `/actuator/health/liveness` | Application state only | Restart a stuck instance |
| `/actuator/health/readiness` | Application state and database connectivity | Remove an instance from the load balancer when it cannot reach the database |
| `/actuator/health` | Summary | Docker Compose health check, AC-E01.2 |

## 6. Alerts (proposed for production)

| Alert | Condition | Severity |
|---|---|---|
| API error rate | 5xx above 1% of requests for 5 minutes | Page |
| Booking latency | p95 `scheduler.booking.duration` above 1 s for 10 minutes | Ticket |
| Contention | `HIGH_CONTENTION` above 0 for 15 minutes | Ticket |
| Readiness | Any instance not ready for 2 minutes | Page |
| Connection pool | Pending connections above 0 for 5 minutes | Ticket |