# T06: Catalog and customer entities

**Prepares:** US-01, US-02, US-03 · **Type:** single task

## Goal

JPA entities and repositories for the reference data, validated against the Flyway schema.

## Read first

- `docs/system-design/03-data-model.md` §1, §2, §4
- `docs/system-design/02-architecture.md` §4
- `src/main/resources/db/migration/V1__reference_tables.sql`

## Files

All under `src/main/java/com/keyloop/scheduler/`:

| Action | File |
|---|---|
| Create | `catalog/Dealership.java`, `catalog/OpeningHours.java`, `catalog/ServiceType.java`, `catalog/Technician.java`, `catalog/ServiceBay.java` |
| Create | `catalog/Skill.java`, `catalog/BayType.java` (enums) |
| Create | `catalog/DealershipRepository.java`, `catalog/ServiceTypeRepository.java`, `catalog/TechnicianRepository.java`, `catalog/ServiceBayRepository.java` |
| Create | `customer/Customer.java`, `customer/Vehicle.java`, `customer/CustomerRepository.java`, `customer/VehicleRepository.java` |
| Create | `src/test/java/com/keyloop/scheduler/catalog/CatalogRepositoryTest.java` |

## Specification

- IDs are `UUID` assigned by the application (no `@GeneratedValue`).
- `Dealership` has `@OneToMany` opening hours (lazy). `OpeningHours` uses an `@EmbeddedId` or `@IdClass` for (`dealership_id`, `day_of_week`), with `DayOfWeek getDayOfWeek()` converting from the ISO number 1–7.
- `Technician` maps skills with `@ElementCollection` on table `technician_skill` (`Set<Skill>`, `@Enumerated(STRING)`).
- `Vehicle` has a `@ManyToOne(fetch = LAZY)` `Customer` and `boolean belongsTo(UUID customerId)`.
- Entities have a protected no-argument constructor for JPA, a public constructor with all required fields, and getters. No setters except where a later task needs them.
- No `equals`/`hashCode` based on mutable fields.

## Tests

`CatalogRepositoryTest` extends `IntegrationTest`:
- The application context starts (this proves `ddl-auto=validate` accepts the mappings).
- D1 has 6 opening-hours entries, Monday to Saturday, 08:00–17:00.
- T1 has skills `GENERAL_MAINTENANCE` and `BRAKES`; T4 is inactive.
- V2 belongs to C2, not C1.

## Watch for (EM)

- `OpeningHours` day mapping: 1 = MONDAY.
- No `FetchType.EAGER`.

## Done when

`.\gradlew.bat test --tests "com.keyloop.scheduler.catalog.CatalogRepositoryTest" --console=plain` passes.
