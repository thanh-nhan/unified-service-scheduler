# T11: Appointment entity, allocation queries, and constraint helper

**Prepares:** US-03 to US-08 · **Type:** single task

## Goal

The persistence building blocks for booking, each proven against real PostgreSQL before any business logic uses them.

## Read first

- `docs/system-design/03-data-model.md` §3, §7
- `docs/system-design/04-booking-and-concurrency.md` §3 and §4 "Rules the implementation must follow" (rule 4 only)
- `src/main/resources/db/migration/V2__appointment.sql`

## Files

| Action | File |
|---|---|
| Create | `booking/Appointment.java`, `booking/AppointmentStatus.java`, `booking/AppointmentRepository.java` |
| Create | `booking/AllocationQueries.java` |
| Create | `common/persistence/ConstraintViolations.java` |
| Create | `src/test/java/com/keyloop/scheduler/booking/AllocationQueriesTest.java` |
| Create | `src/test/java/com/keyloop/scheduler/booking/AppointmentConstraintsTest.java` |

## Specification

**`Appointment`:** maps every column of `appointment` except `time_range`. `@ManyToOne(fetch = LAZY)` to `Customer`, `Vehicle`, `Dealership`, `ServiceType`, `Technician`, `ServiceBay`. No `@Version`. A constructor for a new CONFIRMED appointment (all references, start, end, idempotency key, request hash, `createdAt`).

**`AppointmentRepository`:** `Optional<Appointment> findByCustomerIdAndIdempotencyKey(UUID, String)`, and `Optional<Appointment> findDetailedById(UUID)` with an `@EntityGraph` loading all six references.

**`AllocationQueries`** (`@Repository`, `JdbcClient`), SQL copied from design 04 §3:
- `Optional<UUID> bestTechnician(UUID dealershipId, Skill skill, LocalDateTime start, LocalDateTime end)`; day bounds computed from `start.toLocalDate()`.
- `Optional<UUID> lowestFreeBay(UUID dealershipId, BayType bayType, LocalDateTime start, LocalDateTime end)`.
- `boolean vehicleHasOverlap(UUID vehicleId, LocalDateTime start, LocalDateTime end)`.

**`ConstraintViolations`** (static helpers): walk the cause chain to `org.postgresql.util.PSQLException`.
- `Optional<String> constraintName(Throwable t)` via `getServerErrorMessage().getConstraint()`.
- `boolean isExclusionViolation(Throwable t)`: SQLSTATE `23P01`.
- `boolean isUniqueViolation(Throwable t)`: SQLSTATE `23505`.

## Tests

`AllocationQueriesTest` (extends `IntegrationTest`, uses `seeder`):
- No appointments: `bestTechnician(D1, GENERAL_MAINTENANCE, 09:30, 10:00)` = T1; `lowestFreeBay(D1, GENERAL, ...)` = B1.
- T1 busy 09:00–10:30 → T2 and B2.
- T1 busy 08:00–08:30 on B1, request 11:00–11:30 → T2 (least loaded), B1.
- BRAKES with T1 busy at 09:00–10:30, request 09:30 → empty.
- T4 (inactive) and T5 (other dealership) are never returned.
- Back-to-back: T1 busy 08:30–10:00, BRAKES request 10:00–11:30 → T1.
- A CANCELLED appointment does not block.

`AppointmentConstraintsTest` (extends `IntegrationTest`): seed T1 09:00–10:00, then insert an overlapping appointment directly with `jdbc` for the same technician and catch the exception: `isExclusionViolation` is true and `constraintName` is `ex_appointment_technician_overlap`. Same for bay and vehicle. Also: two rows with the same customer and idempotency key → `isUniqueViolation`, `uq_appointment_idempotency`.

## Watch for (EM)

- SQL matches 04 §3 exactly, including `ORDER BY ... t.id ASC` and the `time_range &&` operator.
- No UUID sorting in Java.

## Done when

`.\gradlew.bat test --tests "com.keyloop.scheduler.booking.*" --console=plain` passes.
