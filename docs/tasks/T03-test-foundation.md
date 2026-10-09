# T03: Fixture data, clock, and test base classes

**Story:** E-01 (AC-E01.4, AC-E01.5, AC-E01.6) · **Type:** single task

## Goal

Every later test depends on this task: one fixture dataset shared by the `dev` profile and the tests, a controllable clock, and helpers for seeding appointments.

## Read first

- `docs/user-stories/00-conventions.md` (sections "Fixture dataset" and "How to read the AC")
- `docs/user-stories/E-01-foundation.md`
- `docs/system-design/03-data-model.md` §6

## Files

| Action | File |
|---|---|
| Create | `src/main/resources/db/fixture/R__fixture_data.sql` |
| Create | `src/main/resources/application-dev.yml` |
| Create | `src/main/java/com/keyloop/scheduler/common/time/ClockConfig.java` |
| Create | `src/test/java/com/keyloop/scheduler/support/Fixture.java` |
| Create | `src/test/java/com/keyloop/scheduler/support/MutableClock.java` |
| Create | `src/test/java/com/keyloop/scheduler/support/IntegrationTest.java` |
| Create | `src/test/java/com/keyloop/scheduler/support/AppointmentSeeder.java` |
| Create | `src/test/java/com/keyloop/scheduler/FoundationTest.java` |
| Modify | the Initializr Testcontainers configuration class, only to use image `postgres:17` |

## Specification

**`R__fixture_data.sql`:** inserts the whole fixture dataset (dealerships, opening hours Monday–Saturday 08:00–17:00, service types, technicians with skills, bays, customers, vehicles) with the exact IDs from `00-conventions.md`. Every `INSERT` ends with `ON CONFLICT DO NOTHING`. Phone numbers may be any value or `NULL`.

**`application-dev.yml`:** `spring.flyway.locations: classpath:db/migration,classpath:db/fixture`. The default (non-dev) locations stay `classpath:db/migration` only.

**`ClockConfig`:** a `@Bean Clock clock()` returning `Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"))`.

**`Fixture`:** `public static final UUID` constants `D1, D2, S1, S2, S3, T1..T5, B1..B4, C1..C3, V1..V3` with the fixture IDs; `LocalDate MONDAY = 2026-10-12`; and `LocalDateTime at(String hhmm)` returning that time on 2026-10-12 (e.g. `at("09:30")`).

**`MutableClock`:** extends `java.time.Clock`, zone `Asia/Ho_Chi_Minh`, with `setNow(LocalDateTime)` and `reset()`. Default instant: `2026-10-11T10:00:00` Vietnam time (GR-09).

**`IntegrationTest`** (abstract base class):
- `@SpringBootTest`, `@AutoConfigureMockMvc`, imports the Testcontainers configuration (one container reused by all tests).
- A `@TestConfiguration` providing a `@Primary` `MutableClock` bean.
- `@BeforeEach`: `TRUNCATE` all tables (`appointment`, `vehicle`, `customer`, `technician_skill`, `technician`, `service_bay`, `opening_hours`, `service_type`, `dealership`) with `RESTART IDENTITY CASCADE`, run `db/fixture/R__fixture_data.sql` (e.g. with `ResourceDatabasePopulator`), and reset the clock.
- Protected fields: `MockMvc mockMvc`, `MutableClock clock`, `JdbcClient jdbc`, `AppointmentSeeder seeder`.

**`AppointmentSeeder`** (inserts directly with `JdbcClient`, bypassing the API):
- `UUID seed(UUID technicianId, UUID bayId, LocalDateTime start, LocalDateTime end)`: creates a **new throwaway customer and vehicle** (random VIN, 17 characters), so seeded appointments never block a fixture vehicle; dealership taken from the technician; service type `S1`; status `CONFIRMED`; random idempotency key; `request_hash` of 64 `'0'` characters; `created_at` = `start` minus 1 day.
- `UUID seed(UUID technicianId, UUID bayId, LocalDateTime start, LocalDateTime end, UUID customerId, UUID vehicleId)`: same, with the given customer and vehicle.
- `void cancel(UUID appointmentId, LocalDateTime at)`: sets `CANCELLED` and `cancelled_at`.

**`FoundationTest`** (extends `IntegrationTest`):
- `AC-E01.4`: dealership and technician counts equal the fixture (2 dealerships, 5 technicians).
- `AC-E01.5`: two tests that each insert one extra dealership and assert the count is 3, proving changes do not leak between tests.
- `AC-E01.6`: after `clock.setNow(2026-10-12T09:00)`, an injected `Clock` returns that time.

## Watch for (EM)

- Fixture IDs exactly match `00-conventions.md` (compare a few by eye).
- T4 is inactive; T5 belongs to D2; B3 is type `EV`.
- Opening hours use `day_of_week` 1–6 only (Sunday closed).

## Done when

`.\gradlew.bat test --tests "com.keyloop.scheduler.FoundationTest" --console=plain` passes.
