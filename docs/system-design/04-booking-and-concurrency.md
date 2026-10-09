# 04: Booking and concurrency

This is the heart of the system. It covers US-03, US-04, US-05, and US-07.

## 1. Guarantees

| # | Guarantee | Enforced by |
|---|---|---|
| G1 | A technician, a bay, or a vehicle is never in two overlapping CONFIRMED appointments | PostgreSQL exclusion constraints (ADR-002). Application checks only pick good candidates |
| G2 | A booking is all-or-nothing | One transaction per attempt: technician, bay, and appointment row commit together or not at all |
| G3 | A retried request never creates a second appointment | `UNIQUE (customer_id, idempotency_key)` plus a request hash (ADR-004) |
| G4 | Losing a race does not fail the customer if other resources are free | Bounded retry with fresh data (§4) |
| G5 | Every appointment's technician and bay belong to its dealership, and its vehicle to its customer | Composite foreign keys (03 §3) |

## 2. Booking algorithm

`BookingService.book(request, idempotencyKey)` is **not** transactional. It runs these steps in order; the first failure returns. Steps 1–9 implement the US-04 check order exactly.

| Step | Check | On failure |
|---|---|---|
| 1 | Body shape: required fields, UUID formats, `startTime` is a local date-time **without an offset** (strict parse, §8) | 400 `VALIDATION_FAILED` |
| 2 | `Idempotency-Key` header present, not blank, at most 100 characters | 400 `IDEMPOTENCY_KEY_MISSING` (blank/missing), 400 `VALIDATION_FAILED` (too long) |
| 2a | **Idempotency replay** (§5): a successful booking already exists for (`customerId`, key) | 200 with the original appointment, or 422 `IDEMPOTENCY_KEY_REUSED` if the body differs |
| 3 | Customer, vehicle, dealership, service type exist (in that order) | 404 with the matching code |
| 4 | Vehicle belongs to the customer | 422 `VEHICLE_NOT_OWNED_BY_CUSTOMER` |
| 5 | `startTime` strictly after `LocalDateTime.now(clock)` | 422 `START_TIME_IN_PAST` |
| 6 | `SchedulingRules.isOnGrid(startTime)` | 422 `START_TIME_NOT_ALIGNED` |
| 7 | `SchedulingRules.fitsOpeningHours(start, end, openingHours)` | 422 `OUTSIDE_OPENING_HOURS` |
| 8 | Vehicle has no overlapping CONFIRMED appointment | 409 `VEHICLE_ALREADY_BOOKED` |
| 9 | Attempt loop (§4): allocate and insert | 201, or 409 `NO_QUALIFIED_TECHNICIAN_AVAILABLE` / `NO_SERVICE_BAY_AVAILABLE` / `VEHICLE_ALREADY_BOOKED` / `HIGH_CONTENTION` |

`end = start + serviceType.durationMinutes`. Step 2a sits before step 3 so a replay returns the original result even if, for example, the start time has since passed.

## 3. Resource selection (GR-06, GR-07)

Both queries run inside the attempt transaction, through `AllocationQueries` (native SQL with `JdbcClient`). `:start` and `:end` are the requested interval; `:dayStart` and `:dayEnd` are midnight of the appointment's date and the following midnight.

```sql
-- Best qualified technician: free for the whole interval, least booked minutes that day, then lowest id
SELECT t.id
FROM technician t
JOIN technician_skill ts ON ts.technician_id = t.id AND ts.skill = :requiredSkill
WHERE t.dealership_id = :dealershipId
  AND t.active
  AND NOT EXISTS (
        SELECT 1 FROM appointment a
        WHERE a.technician_id = t.id
          AND a.status = 'CONFIRMED'
          AND a.time_range && tsrange(:start, :end, '[)'))
ORDER BY (
        SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (a.end_time - a.start_time))), 0)
        FROM appointment a
        WHERE a.technician_id = t.id
          AND a.status = 'CONFIRMED'
          AND a.start_time >= :dayStart AND a.start_time < :dayEnd) ASC,
      t.id ASC
LIMIT 1;

-- Lowest-id eligible bay that is free for the whole interval
SELECT b.id
FROM service_bay b
WHERE b.dealership_id = :dealershipId
  AND b.bay_type = :requiredBayType
  AND b.active
  AND NOT EXISTS (
        SELECT 1 FROM appointment a
        WHERE a.service_bay_id = b.id
          AND a.status = 'CONFIRMED'
          AND a.time_range && tsrange(:start, :end, '[)'))
ORDER BY b.id ASC
LIMIT 1;
```

- The technician is chosen first. If none is free, the result is `NO_QUALIFIED_TECHNICIAN_AVAILABLE` even if no bay is free either (AC-03.5, AC-03.6).
- "Lowest id" means PostgreSQL's `uuid` ordering. **Never sort UUIDs in Java** for this: `UUID.compareTo` compares signed numbers and can give a different order.
- Example (AC-03.3): T1 has 30 booked minutes that day, T2 has 0, both qualified and free, so T2 wins. B1 is free and has the lowest id, so B1 is chosen.

## 4. Concurrency: attempt loop and retries

### Why application checks are not enough

The selection queries are a *check*; the insert is the *act*. Two requests can both see technician T1 free, and both insert. This "check, then insert" race is the classic double-booking bug. The exclusion constraints close the gap: PostgreSQL checks overlaps as part of the `INSERT` and serializes conflicting inserts.

### What happens in a race

| Time | Request A (09:00–10:00) | Request B (09:30–10:00) |
|---|---|---|
| t1 | Selects T1, B1 | Selects T1, B2 (A not committed yet) |
| t2 | `INSERT` T1/B1 | `INSERT` T1/B2: **waits**, because A's uncommitted row conflicts on T1 |
| t3 | `COMMIT` → 201 | Wakes up: A committed, so `SQLSTATE 23P01` on `ex_appointment_technician_overlap`. Rollback |
| t4 | | Attempt 2 in a **new** transaction. Selection now sees A's row, picks T2, inserts → 201 |

PostgreSQL's default READ COMMITTED isolation gives each statement in a new transaction a fresh view, so attempt 2 sees A's committed row. No exclusion list is needed.

### Pseudocode

```text
BookingService.book(...)                      // NOT @Transactional
    ... steps 1–8 ...
    for attempt in 1..3:
        try:
            appointment = bookingTransaction.tryAllocateAndInsert(cmd)   // separate bean, @Transactional
            return created(appointment)                                   // 201
        catch NoCapacityException e:
            return conflict(e.code)                                       // 409, no retry
        catch DataIntegrityViolationException e:
            replay = idempotency.findSuccessful(customerId, key)          // ALWAYS check first, see below
            if replay present: return replayResult(replay, requestHash)   // 200 or 422
            switch ConstraintViolations.constraintName(e):
                "ex_appointment_vehicle_overlap"     -> return conflict(VEHICLE_ALREADY_BOOKED)
                "ex_appointment_technician_overlap",
                "ex_appointment_bay_overlap"         -> metrics.retry(name); continue
                default                              -> throw e            // unexpected → 500
    return conflict(HIGH_CONTENTION)

BookingTransaction.tryAllocateAndInsert(cmd)  // @Transactional (READ COMMITTED)
    techId = allocation.bestTechnician(...)  ?: throw NoCapacity(NO_QUALIFIED_TECHNICIAN_AVAILABLE)
    bayId  = allocation.lowestFreeBay(...)   ?: throw NoCapacity(NO_SERVICE_BAY_AVAILABLE)
    return appointmentRepository.saveAndFlush(new Appointment(..., CONFIRMED, key, requestHash, now))
```

### Rules the implementation must follow

1. **The retry loop is outside the transaction.** After `23P01`, PostgreSQL aborts the transaction; every later statement in it fails. Each attempt must be a new transaction, so `tryAllocateAndInsert` lives in a **separate Spring bean**. Calling a `@Transactional` method from the same class bypasses the proxy and silently runs without a new transaction.
2. **Do not catch the constraint exception inside the transactional method.** Doing so leaves the transaction marked rollback-only and causes `UnexpectedRollbackException`.
3. **Use `saveAndFlush`.** A plain `save` can delay the `INSERT` until commit, so the violation would surface outside the expected place.
4. **Identify violations by SQLSTATE and constraint name**, not by message text. `ConstraintViolations` walks the cause chain to `org.postgresql.util.PSQLException` and reads `getSQLState()` (`23P01` exclusion, `23505` unique) and `getServerErrorMessage().getConstraint()`.
5. **Check idempotency before interpreting a violation.** Two identical requests with the same key (AC-05.5) conflict on the idempotency unique key *and* on the vehicle and technician constraints. PostgreSQL does not guarantee which is reported first. Looking up the key first makes the result deterministic: the loser always replays the winner's appointment.
6. **Maximum 3 attempts.** Conflicts are rare in normal use. The bound stops a hot slot from hammering the database.

### Alternatives considered

Pessimistic locking (`SELECT … FOR UPDATE` on technician and bay rows), advisory locks, SERIALIZABLE isolation, and MySQL-based approaches are compared in [ADR-002](adr/ADR-002-database-enforced-overlap.md).

## 5. Idempotency

See ADR-004 for the decision. Algorithm:

1. `requestHash = SHA-256 hex` of `customerId|vehicleId|dealershipId|serviceTypeId|startTime`, with `startTime` formatted as `yyyy-MM-dd'T'HH:mm:ss`.
2. **Lookup** (step 2a and after any violation): `SELECT … FROM appointment WHERE customer_id = :customerId AND idempotency_key = :key`.
   - Not found → continue.
   - Found and `request_hash` equal → **200** with the stored appointment (same body as the original 201).
   - Found and hash different → **422** `IDEMPOTENCY_KEY_REUSED`.
3. Only successful bookings are stored, because the key lives on the appointment row. A rejected request leaves nothing behind, so retrying it is evaluated fresh (AC-05.6).
4. A replay of a booking that has since been cancelled returns 200 with the current (CANCELLED) state. This is not covered by an AC; it is the least surprising behavior.

## 6. Cancellation (US-07)

```sql
UPDATE appointment
SET status = 'CANCELLED', cancelled_at = :now
WHERE id = :id AND status = 'CONFIRMED' AND start_time > :now;
```

| Rows updated | Then | Response |
|---|---|---|
| 1 | Load the appointment | 200 CANCELLED |
| 0 | Load by id: not found | 404 `APPOINTMENT_NOT_FOUND` |
| 0 | Load by id: already CANCELLED | 200 with the **original** `cancelledAt` (AC-07.4) |
| 0 | Load by id: CONFIRMED, start time not in the future | 422 `APPOINTMENT_ALREADY_STARTED` |

Concurrent cancels (AC-07.7): the second `UPDATE` waits for the first's row lock, re-evaluates its `WHERE` clause after the commit, updates 0 rows, and returns the already-cancelled appointment. Both responses are 200 with the same `cancelledAt`. No optimistic locking or retry is needed.

## 7. Reading appointments (US-06, US-08)

- `GET /appointments/{id}` loads the appointment with an entity graph and maps it to the shared representation.
- `GET /customers/{id}/appointments` checks the customer exists (404 otherwise), then runs one paged query on `ix_appointment_customer_start`, sorted by `start_time, id`, with an optional status filter.
- Responses never include customer email or phone (GR-12). The DTO simply has no such fields.

## 8. Date-time parsing

`startTime` is bound as a `String` and parsed with `LocalDateTime.parse(value)` (`ISO_LOCAL_DATE_TIME`). This rejects any trailing offset (`+07:00`) or `Z` with an exception, which is mapped to 400 `VALIDATION_FAILED` with an `errors` entry for `startTime` (AC-04.2). Relying on Jackson's default `LocalDateTime` handling is not safe, because lenient settings can accept a trailing `Z` and silently drop it. Seconds are accepted at this stage, so `09:15:30` fails later with 422 `START_TIME_NOT_ALIGNED`, as AC-04.8 requires.
