# T14: Concurrency and idempotency

**Story:** US-05 (AC-05.1 to AC-05.6) · **Parts:** A, B

This is the showpiece of the submission. Review it more carefully than any other task.

## Read first (both parts)

- `docs/user-stories/US-05-reliable-booking.md`
- `src/test/java/com/keyloop/scheduler/acceptance/BookingApi.java`, `support/Fixture.java`, `support/AppointmentSeeder.java`

---

## Part A: acceptance tests

**Create:** `src/test/java/com/keyloop/scheduler/acceptance/ReliableBookingAcceptanceTest.java`.

**Concurrency pattern** (use for AC-05.1, AC-05.2, AC-05.5):

```java
int n = 20;
ExecutorService pool = Executors.newFixedThreadPool(n);
CountDownLatch ready = new CountDownLatch(n);
CountDownLatch go = new CountDownLatch(1);
List<Future<MvcResult>> results = new ArrayList<>();
for (int i = 0; i < n; i++) {
    final int idx = i;
    results.add(pool.submit(() -> {
        ready.countDown();
        go.await();
        return booking.book(customers[idx], vehicles[idx], D1, S3, "2026-10-12T09:00:00").andReturn();
    }));
}
ready.await();
go.countDown();
// collect statuses with future.get(30, SECONDS)
```

If concurrent `MockMvc` calls show errors unrelated to the business logic, stop and tell the EM; the fallback is a real HTTP server (`webEnvironment = RANDOM_PORT`) with `java.net.http.HttpClient`.

Rules:
- AC-05.1: create 20 extra customers, each with one vehicle, with `jdbc` in the test. Assert exactly one 201, nineteen 409 with a code in the allowed set, no 5xx, and exactly one CONFIRMED appointment for T3 overlapping 09:00–10:00 (SQL count).
- AC-05.2: loop 10 rounds at 10 different start times (e.g. 08:00, 08:30, … 12:30); each round books C1/V1 and C2/V2 concurrently; both 201 with different technicians and bays.
- AC-05.5: 5 concurrent requests by C1 with the same key `K2` and identical body; all 200 or 201, one distinct appointment id, one row.
- AC-05.6: book C2 BRAKE_SERVICE at 09:30 with key `K3` while T1 is busy (seeded) → 409; cancel the seeded appointment with `seeder.cancel`; repeat with `K3` → 201.

**Done when:** compiles; AC-05.3, AC-05.4, and AC-05.5 fail (no replay yet). **Then (EM):** review, freeze, commit. Run the class 3 times; concurrency tests must not be flaky.

---

## Part B: implementation

**Also read:** `docs/system-design/04-booking-and-concurrency.md` §2 (step 2a), §4 (rules 5 and 6), §5; `booking/BookingService.java`, `booking/RequestHasher.java`.

**Files:**

| Action | File |
|---|---|
| Create | `booking/IdempotencyService.java` (lookup by customer and key; compare hash; 200 replay or 422) |
| Modify | `booking/BookingService.java`, `booking/BookingController.java` (return 200 for a replay, 201 for a new booking) |
| Create | `booking/BookingResult.java` (record: appointment + `boolean replayed`) |

**Specification:** follow 04 §4 pseudocode exactly: step 2a before existence checks; after ANY `DataIntegrityViolationException`, look up the key first, then decide by constraint name; vehicle constraint → 409 `VEHICLE_ALREADY_BOOKED`; technician or bay constraint → retry; unknown constraint → rethrow; after 3 lost races → 409 `HIGH_CONTENTION`.

**Watch for (EM):**
1. The idempotency lookup after a violation runs outside the failed transaction (it is called from `BookingService`, not from inside `BookingTransaction`).
2. A replay returns the same body as the original 201 (same `createdAt`).
3. Failed requests store nothing.

**Done when:** `ReliableBookingAcceptanceTest` passes unchanged 3 runs in a row, and all earlier acceptance tests pass.
