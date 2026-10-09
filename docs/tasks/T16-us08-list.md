# T16: List my appointments

**Story:** US-08 (AC-08.1 to AC-08.5) · **Priority:** Should · **Parts:** A, B

## Read first (both parts)

- `docs/user-stories/US-08-list-my-appointments.md`
- `src/test/java/com/keyloop/scheduler/support/IntegrationTest.java`, `support/Fixture.java`, `support/AppointmentSeeder.java`

---

## Part A: acceptance tests

**Create:** `src/test/java/com/keyloop/scheduler/acceptance/ListAppointmentsAcceptanceTest.java`.

- Seed with `seeder.seed(tech, bay, start, end, customerId, vehicleId)` so appointments belong to C1 or C2.
- AC-08.3: 25 non-overlapping appointments for C1/V1 (for example 30 minutes each across several days, alternating T1/T2 and B1/B2), then `page=2&size=10`.
- AC-08.4 as a parameterized test.

**Done when:** compiles, tests fail with 404. **Then (EM):** review, freeze, commit.

---

## Part B: implementation

**Also read:** `docs/system-design/04-booking-and-concurrency.md` §7; `booking/AppointmentRepository.java`, `booking/AppointmentMapper.java`.

**Files:**

| Action | File |
|---|---|
| Create | `booking/CustomerAppointmentsController.java` |
| Modify | `booking/AppointmentQueryService.java`, `booking/AppointmentRepository.java` |
| Create | `booking/dto/AppointmentPageResponse.java` |

**Specification:** `GET /api/v1/customers/{customerId}/appointments?status=&page=0&size=20`; 404 `CUSTOMER_NOT_FOUND` for an unknown customer; `@Min(0)` page, `@Min(1) @Max(100)` size; sort `startTime` then `id`; the entity graph avoids N+1 queries.

**Done when:** the frozen class passes unchanged, and all earlier acceptance tests pass.
