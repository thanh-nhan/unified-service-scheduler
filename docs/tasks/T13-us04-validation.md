# T13: Booking validation order

**Story:** US-04 (AC-04.1 to AC-04.11) · **Parts:** A, B

## Read first (both parts)

- `docs/user-stories/US-04-booking-validation.md`
- `src/test/java/com/keyloop/scheduler/acceptance/BookingApi.java`, `support/Fixture.java`

---

## Part A: acceptance tests

**Create:** `src/test/java/com/keyloop/scheduler/acceptance/BookingValidationAcceptanceTest.java`.

- One test per AC; tables (AC-04.5, AC-04.9) as `@ParameterizedTest` with `@CsvSource` or `@MethodSource`.
- AC-04.3: send a raw invalid body such as `{"customerId":` with content type JSON.
- AC-04.4: build the request without the header (do not use `BookingApi`'s helper that adds a key).
- AC-04.11: assert the appointment count is unchanged for a representative rejected request from each of AC-04.1 to AC-04.10 (parameterized).

**Done when:** compiles; some tests may already pass because T12 implemented the order. **Then (EM):** review, freeze, commit.

---

## Part B: implementation

**Also read:** `docs/system-design/04-booking-and-concurrency.md` §2, §8; `booking/BookingService.java`, `booking/dto/BookAppointmentRequest.java`.

**Files:** modify only `booking/BookingService.java`, `booking/dto/BookAppointmentRequest.java`, `booking/BookingController.java`, and `common/error/GlobalExceptionHandler.java` if a mapping is missing.

**Specification:** make every frozen test pass while keeping the exact order in 04 §2. Bean Validation (`@NotNull`) on request fields covers AC-04.1; field name in `errors` must be the JSON name (`serviceTypeId`).

**Watch for (EM):** the order of checks in `BookingService` reads top to bottom like the US-04 list; no check is duplicated in the controller.

**Done when:** `BookingValidationAcceptanceTest` and `BookAppointmentAcceptanceTest` pass unchanged.
