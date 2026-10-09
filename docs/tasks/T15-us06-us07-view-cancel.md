# T15: View and cancel an appointment

**Stories:** US-06 (AC-06.1 to AC-06.5), US-07 (AC-07.1 to AC-07.7) · **Parts:** A, B

## Read first (both parts)

- `docs/user-stories/US-06-view-appointment.md`
- `docs/user-stories/US-07-cancel-appointment.md`
- `src/test/java/com/keyloop/scheduler/acceptance/BookingApi.java`, `support/Fixture.java`, `support/AppointmentSeeder.java`

---

## Part A: acceptance tests

**Create:**
- `src/test/java/com/keyloop/scheduler/acceptance/ViewAppointmentAcceptanceTest.java`
- `src/test/java/com/keyloop/scheduler/acceptance/CancelAppointmentAcceptanceTest.java`

Rules:
- AC-06.1: compare the GET body field by field with the booking response body.
- AC-06.5: assert the JSON text contains neither `"email"` nor `"phone"`.
- AC-07.4: cancel, then `clock.setNow(2026-10-11T11:00)`, cancel again, and assert `cancelledAt` is still `2026-10-11T10:00:00`.
- AC-07.5: seed the appointment at 09:00, set the clock to 2026-10-12 09:00, cancel → 422.
- AC-07.7: two concurrent cancels with the latch pattern from T14.

**Done when:** compiles, tests fail with 404/405. **Then (EM):** review, freeze, commit.

---

## Part B: implementation

**Also read:** `docs/system-design/04-booking-and-concurrency.md` §6, §7; `booking/AppointmentRepository.java`, `booking/AppointmentMapper.java`, `booking/BookingController.java`.

**Files:**

| Action | File |
|---|---|
| Create | `booking/CancellationService.java` |
| Create | `booking/AppointmentQueryService.java` (view by id) |
| Modify | `booking/BookingController.java` (add `GET /api/v1/appointments/{id}` and `POST /api/v1/appointments/{id}/cancel`) |
| Modify | `booking/AppointmentRepository.java` (add the conditional cancel update with `@Modifying` native or JPQL query) |

**Specification:** cancellation is the single conditional `UPDATE` in 04 §6, followed by the decision table. No optimistic locking, no retry.

**Watch for (EM):** the update's `WHERE` includes `status = 'CONFIRMED' AND start_time > :now`; the already-cancelled branch returns the stored `cancelledAt`, not a new one.

**Done when:** both frozen classes pass unchanged, and all earlier acceptance tests pass.
