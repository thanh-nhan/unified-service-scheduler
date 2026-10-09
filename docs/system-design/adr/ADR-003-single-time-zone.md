# ADR-003: Single time zone (Vietnam local time)

**Status:** Accepted by PO (user stories v0.2) · **Date:** 2026-10-08

## Context

All dealerships and customers are in Vietnam (UTC+07:00, no daylight saving). An earlier design stored UTC `timestamptz`, kept a time zone per dealership, and stepped through DST transitions carefully. The PO removed multi-time-zone support from scope.

## Decision

- Every date-time is Vietnam local time, stored as PostgreSQL `timestamp` (without time zone) and mapped to `LocalDateTime`.
- The API accepts and returns local date-times **without an offset**; a value with an offset or `Z` is rejected (400).
- The application `Clock` bean uses `Asia/Ho_Chi_Minh`. The container also sets `TZ=Asia/Ho_Chi_Minh` so logs read naturally.
- SQL never calls `now()`; all times come from the application clock.

## Consequences

- No conversion code, no DST edge cases, simpler tests and API.
- Rejecting offsets keeps the contract unambiguous instead of silently converting.
- Expanding to other countries later needs a migration to `timestamptz` plus a dealership time zone (09 §6). The `Clock` and strict parsing are already isolated in `common.time`, which limits the change.

## Alternatives considered

| Option | Why not |
|---|---|
| UTC `timestamptz` with per-dealership time zones | Correct for multiple countries, but complexity with no current business need |
| Accept any offset and convert to Vietnam time | Friendlier, but hides client bugs and adds conversion rules to test |
