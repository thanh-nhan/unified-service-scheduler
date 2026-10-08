# US-05: Reliable booking under concurrency and retries

**Priority:** Must · **Depends on:** US-03 · **Status:** Draft

## Story

**As a** vehicle owner booking on a phone with an unreliable connection
**I want** my booking to be safe to retry and never double-booked
**So that** I get exactly one appointment, and the dealership never has two cars in one bay

## Business rules

- No two CONFIRMED appointments may overlap for the same technician, the same bay, or the same vehicle, **including when requests arrive at the same moment**.
- If a booking loses a race for a resource, the system retries with other eligible resources. After 3 attempts, it returns 409 `HIGH_CONTENTION`.
- **Idempotency.** Only **successful** bookings are remembered by their `Idempotency-Key`.
  - Same key and same body: return the original appointment with 200, without creating a new one.
  - Same key and a different body: 422 `IDEMPOTENCY_KEY_REUSED`.
  - A request that was rejected (4xx) is not remembered, so retrying it is evaluated again.

## Acceptance criteria

**AC-05.1: Concurrent requests for the last slot**
Given 20 additional customers, each owning one vehicle
And T3 and B3 are the only EV resources at D1
When all 20 customers concurrently book EV_BATTERY_CHECK starting 09:00, each with its own key
Then exactly 1 response is 201
And 19 responses are 409 with code `NO_QUALIFIED_TECHNICIAN_AVAILABLE`, `NO_SERVICE_BAY_AVAILABLE`, or `HIGH_CONTENTION`
And exactly 1 CONFIRMED appointment exists for T3 overlapping 09:00–10:00
And no response is 5xx

**AC-05.2: A lost race is resolved by retrying with another resource**
Given no appointments
When C1 (V1) and C2 (V2) concurrently book OIL_CHANGE at the same start time
Then both responses are 201
And they have different technicians and different bays
And this holds for each of 10 rounds at 10 different start times

**AC-05.3: Idempotent replay**
Given C1 booked successfully with key `K1`
When the identical request is sent again with key `K1`
Then the response is 200 with the same appointment `id` and body
And exactly 1 appointment exists for that request

**AC-05.4: Key reused with a different body**
Given C1 booked successfully with key `K1` starting 09:30
When C1 sends key `K1` with `startTime` 11:00
Then the response is 422 `IDEMPOTENCY_KEY_REUSED`
And no new appointment is stored

**AC-05.5: Concurrent duplicates**
When 5 identical requests with key `K2` are sent concurrently
Then every response is 201 or 200
And all responses contain the same appointment `id`
And exactly 1 appointment is stored

**AC-05.6: A rejected request can be retried**
Given C2's BRAKE_SERVICE request at 09:30 with key `K3` was rejected with 409 because T1 was busy
And T1's blocking appointment is then cancelled
When C2 sends the identical request with key `K3`
Then the response is 201

## Notes for implementer

- A "check, then insert" in application code alone does **not** satisfy AC-05.1. Follow the approved design (database-enforced overlap prevention).
- Each retry attempt must run in a **new** transaction. The retry loop must not be inside the `@Transactional` method.
- These tests must use real concurrency (e.g. `ExecutorService` + `CountDownLatch`) against Testcontainers PostgreSQL.
