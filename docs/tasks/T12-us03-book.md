# T12: Book an appointment

**Story:** US-03 (AC-03.1 to AC-03.10) · **Parts:** A, B

## Read first (both parts)

- `docs/user-stories/US-03-book-appointment.md`
- `docs/user-stories/00-conventions.md` ("Appointment representation" only)
- `src/test/java/com/keyloop/scheduler/support/IntegrationTest.java`, `support/Fixture.java`, `support/AppointmentSeeder.java`

---

## Part A: acceptance tests

**Create:**
- `src/test/java/com/keyloop/scheduler/acceptance/BookingApi.java`: a small helper used by all booking tests: `ResultActions book(UUID customer, UUID vehicle, UUID dealership, UUID serviceType, String startTime)` (sends a fresh random `Idempotency-Key`) and an overload taking the key.
- `src/test/java/com/keyloop/scheduler/acceptance/BookAppointmentAcceptanceTest.java`.

Rules:
- One test per AC. AC-03.6 creates the extra technician T6 with `jdbc` inside the test.
- Assert exact technician and bay IDs (`jsonPath("$.technician.id").value(T2.toString())`), status codes, error `code`, and appointment counts (`select count(*) from appointment` before and after).
- AC-03.5: also assert `detail` is not empty and `$.alternatives` does not exist.

**Done when:** compiles, tests fail with 404/405. **Then (EM):** review, freeze both files, commit.

---

## Part B: implementation

**Also read:** `docs/system-design/04-booking-and-concurrency.md` §2, §3, §4 (pseudocode and rules), §8; `booking/AllocationQueries.java`.

**Files:**

| Action | File |
|---|---|
| Create | `booking/BookingService.java` (NOT `@Transactional`) |
| Create | `booking/BookingTransaction.java` (separate bean, `@Transactional` method `tryAllocateAndInsert`) |
| Create | `booking/BookingCommand.java` (record), `booking/NoCapacityException.java` |
| Create | `booking/BookingController.java` |
| Create | `booking/dto/BookAppointmentRequest.java`, `booking/dto/AppointmentResponse.java` (+ nested records for references) |
| Create | `booking/AppointmentMapper.java` |

**Scope of this part:** steps 1, 3–9 of 04 §2 with the full attempt loop, but **without idempotency replay** (T14 adds it; the key is still required and stored, and `request_hash` is computed with `RequestHasher` from 04 §5, created here as `booking/RequestHasher.java`). Validation order is tested in T13, but implement the order from 04 §2 now.

**Specification highlights:**
- `startTime` is a `String` in the request record, parsed with `LocalDateTimes.parse("startTime", ...)`.
- 201 with `Location: /api/v1/appointments/{id}` and the appointment representation; times via `LocalDateTimes.format`.
- Technician first, then bay; `NO_QUALIFIED_TECHNICIAN_AVAILABLE` before `NO_SERVICE_BAY_AVAILABLE`.
- Vehicle pre-check (step 8) before the attempt loop → 409 `VEHICLE_ALREADY_BOOKED`.

**Watch for (EM), check each one in the diff:**
1. `@Transactional` is on `BookingTransaction`, not on `BookingService`, and `BookingService` calls it through the injected bean.
2. The transactional method does not catch exceptions.
3. `saveAndFlush` is used.
4. The retry loop is in `BookingService`, max 3 attempts, and reads the constraint name via `ConstraintViolations`.
5. No customer email or phone in `AppointmentResponse`.

**Done when:** the frozen `BookAppointmentAcceptanceTest` passes unchanged, and all earlier acceptance tests still pass.
