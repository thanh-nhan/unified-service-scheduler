# T10: Weekly availability

**Story:** US-09 (AC-09.1 to AC-09.6) · **Parts:** A, B

## Read first (both parts)

- `docs/user-stories/US-09-weekly-availability.md`
- `src/test/java/com/keyloop/scheduler/support/IntegrationTest.java`, `support/Fixture.java`, `support/AppointmentSeeder.java`

---

## Part A: acceptance tests

**Create:** `src/test/java/com/keyloop/scheduler/acceptance/WeeklyAvailabilityAcceptanceTest.java`.

- One test per AC; AC-09.6 as a parameterized test over its table.
- AC-09.4: call the daily endpoint for each of the 7 dates and compare its `slots` array to the weekly day's `slots` (parse both with the test's JSON mapper or compare `jsonPath` lists).

**Done when:** compiles, tests fail with 404. **Then (EM):** review, freeze, commit.

---

## Part B: implementation

**Also read:** `docs/system-design/05-availability.md` §3, §4; `availability/AvailabilityService.java`, `availability/AvailabilityController.java`.

**Files:**

| Action | File |
|---|---|
| Modify | `availability/AvailabilityService.java`, `availability/AvailabilityController.java` |
| Create | `availability/dto/WeeklyAvailabilityResponse.java`, `availability/dto/DayAvailabilityResponse.java` |

**Specification:**
- `GET /api/v1/dealerships/{dealershipId}/availability/weekly?serviceTypeId={uuid}&weekStart={yyyy-MM-dd}` (`weekStart` optional).
- Omitted `weekStart` → `LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(MONDAY))`.
- `weekStart` not a Monday → 400 `VALIDATION_FAILED` with `errors[0].field = "weekStart"`.
- Load data once for `[weekStart 00:00, weekStart + 7 days 00:00)`; call the calculator once per open day; closed days get `open: false`, `slots: []`.
- Always 7 entries, Monday to Sunday.

**Watch for (EM):** the daily endpoint still passes its frozen tests; no copy of the calculator logic.

**Done when:** `WeeklyAvailabilityAcceptanceTest` and `DailyAvailabilityAcceptanceTest` both pass unchanged.
