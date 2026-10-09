# E-02: Observability (enabler)

**Priority:** Must (AC-E02.6 is Should) · **Depends on:** US-03 · **Status:** Approved (v0.3)

## Story

**As** the operations team
**I want to** trace, measure, and diagnose booking activity
**So that** problems are detected quickly and any request can be investigated end to end

## Acceptance criteria

**AC-E02.1: Request ID propagation**
When a request includes header `X-Request-Id: abc-123`
Then the response includes header `X-Request-Id: abc-123`
When a request has no `X-Request-Id`
Then the response includes a generated UUID in `X-Request-Id`

**AC-E02.2: Request ID in error bodies**
When any request fails with a problem response
Then the body's `requestId` equals the response's `X-Request-Id` header

**AC-E02.3: Structured logs**
Given the `dev` profile
Then logs are written as one JSON object per line, with at least `timestamp`, `level`, `logger`, `message`, and `requestId`

**AC-E02.4: Booking outcome log event**
When a booking request completes, whether it succeeds or fails
Then exactly one INFO log event `booking.completed` is written with `outcome` (`CONFIRMED` or the error code), `appointmentId` (on success), `dealershipId`, `serviceTypeCode`, and `durationMs`
And the event contains no customer name, email, phone, or VIN

**AC-E02.5: Business metrics**
Given one successful booking and one booking rejected with `NO_QUALIFIED_TECHNICIAN_AVAILABLE`
When the client calls `GET /actuator/prometheus`
Then the output contains:
- `scheduler_booking_attempts_total{outcome="CONFIRMED"} 1.0`
- `scheduler_booking_attempts_total{outcome="NO_QUALIFIED_TECHNICIAN_AVAILABLE"} 1.0`
- a histogram `scheduler_booking_duration_seconds`
- a counter `scheduler_booking_conflict_retries_total` with a `constraint` label

(Micrometer names: `scheduler.booking.attempts`, `scheduler.booking.duration`, `scheduler.booking.conflict.retries`.)

**AC-E02.6 (Should): Health probes and tracing**
Then `/actuator/health/liveness` and `/actuator/health/readiness` return UP, and readiness includes the database check
And when tracing is enabled, log lines include `traceId` and `spanId`
