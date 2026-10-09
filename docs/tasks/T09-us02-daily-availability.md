# T09: Daily availability

**Story:** US-02 (AC-02.1 to AC-02.10) · **Parts:** A, B

## Read first (both parts)

- `docs/user-stories/US-02-check-availability.md`
- `src/test/java/com/keyloop/scheduler/support/IntegrationTest.java`, `support/Fixture.java`, `support/AppointmentSeeder.java`

---

## Part A: acceptance tests

**Create:** `src/test/java/com/keyloop/scheduler/acceptance/DailyAvailabilityAcceptanceTest.java`.

- One test per AC (AC-02.10 as a `@ParameterizedTest` or four tests). `@DisplayName` starts with the AC ID.
- Seed appointments with `seeder.seed(...)`; cancelled ones with `seeder.cancel(...)`.
- Set the clock with `clock.setNow(...)` where the AC says so.
- Assert exact slot counts, first and last slot times (`"2026-10-12T08:00:00"`), and presence or absence of specific start times.

**Done when:** compiles, tests fail with 404. **Then (EM):** review, freeze, commit.

---

## Part B: implementation

**Also read:** `docs/system-design/05-availability.md` §3, §4; `availability/AvailabilityCalculator.java`.

**Files:**

| Action | File |
|---|---|
| Create | `availability/AvailabilityService.java`, `availability/AvailabilityController.java` |
| Create | `availability/AvailabilityQueries.java` (`JdbcClient`: qualified technician ids, eligible bay ids, busy intervals in a range) |
| Create | `availability/dto/DailyAvailabilityResponse.java`, `availability/dto/SlotResponse.java` |

**Specification:**
- `GET /api/v1/dealerships/{dealershipId}/availability?serviceTypeId={uuid}&date={yyyy-MM-dd}`.
- Exactly the 4 data loads in 05 §3; one calculator call; never one query per slot.
- Busy intervals query: CONFIRMED appointments overlapping `[from, to)` where `technician_id IN (:techIds) OR service_bay_id IN (:bayIds)`; skip the query when both sets are empty.
- Times formatted with `LocalDateTimes.format`.
- Unknown dealership or service type → `ApiException` with the 404 codes. Missing or malformed parameters are handled by the global handler.
- The service method takes the dealership, service type, and date range so T10 can reuse it for a week.

**Watch for (EM):** no slot logic outside `AvailabilityCalculator`; `now` comes from `LocalDateTime.now(clock)` in the service.

**Done when:** the frozen `DailyAvailabilityAcceptanceTest` passes unchanged.
