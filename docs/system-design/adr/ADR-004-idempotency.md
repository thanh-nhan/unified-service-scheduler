# ADR-004: Idempotent booking with keys stored on the appointment

**Status:** Proposed · **Date:** 2026-10-08

## Context

Customers book from phones on unreliable networks. A client that times out cannot know whether its booking succeeded, and a blind retry must not create a second appointment (US-05).

## Decision

- `POST /appointments` requires an `Idempotency-Key` header (up to 100 characters).
- On success, the key and a SHA-256 hash of the request fields are stored **on the appointment row**, with `UNIQUE (customer_id, idempotency_key)`.
- A repeated key with the same hash returns 200 and the original appointment; with a different hash, 422 `IDEMPOTENCY_KEY_REUSED`.
- Rejected requests are not stored, so a retry after a 4xx is evaluated fresh.
- After any constraint violation, the key is looked up first, which makes concurrent duplicates deterministic (04 §4).

## Consequences

- No extra table, no cleanup job, and the key is written atomically with the booking.
- Keys are scoped per customer, so two customers can use the same key value without interfering.
- Failed requests are not remembered. Acceptable, because re-evaluating a rejected request is safe.

## Alternatives considered

| Option | Why not |
|---|---|
| Separate `idempotency_record` table storing every response (including errors) | More general, but needs a time-to-live cleanup and a second write; replaying stored errors is not required |
| No idempotency; rely on the vehicle overlap constraint | A retry would get 409 `VEHICLE_ALREADY_BOOKED` instead of its own appointment, confusing the client |
