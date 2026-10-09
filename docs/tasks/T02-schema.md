# T02: Database schema migrations

**Story:** E-01 (AC-E01.3) · **Type:** single task

## Goal

Create the complete schema with Flyway, exactly as specified in the design, including the three exclusion constraints that prevent double-booking.

## Read first

- `docs/system-design/03-data-model.md` (whole file)

## Files

| Action | File |
|---|---|
| Create | `src/main/resources/db/migration/V1__reference_tables.sql` |
| Create | `src/main/resources/db/migration/V2__appointment.sql` |
| Create | `src/main/resources/application.yml` (replace `application.properties` if present) |
| Create | `src/test/java/com/keyloop/scheduler/SchemaTest.java` |

## Steps

1. `V1__reference_tables.sql`: `CREATE EXTENSION IF NOT EXISTS btree_gist;` then tables `dealership`, `opening_hours`, `service_type`, `technician`, `technician_skill`, `service_bay`, `customer`, `vehicle`, with every constraint and index from 03 §4 and §5, including `UNIQUE (id, dealership_id)` on `technician` and `service_bay`, and `UNIQUE (id, customer_id)` on `vehicle`.
2. `V2__appointment.sql`: the `appointment` table **exactly** as in 03 §3 (copy the constraint names character for character), plus `ix_appointment_dealership_start` and `ix_appointment_customer_start`.
3. `application.yml` (minimal for now):

   ```yaml
   spring:
     application:
       name: scheduler
     jpa:
       open-in-view: false
       hibernate:
         ddl-auto: validate
   ```

4. `SchemaTest`: a `@SpringBootTest` that uses the generated Testcontainers configuration and asserts, by querying `pg_constraint` with `JdbcClient`, that these constraints exist: `ex_appointment_technician_overlap`, `ex_appointment_bay_overlap`, `ex_appointment_vehicle_overlap`, `uq_appointment_idempotency`, `fk_appointment_technician`, `fk_appointment_bay`, `fk_appointment_vehicle`.

## Watch for (EM)

- `time_range tsrange GENERATED ALWAYS AS (tsrange(start_time, end_time, '[)')) STORED`.
- All three exclusion constraints have `WHERE (status = 'CONFIRMED')`.
- Columns are `timestamp`, not `timestamptz`. No `DEFAULT now()` anywhere.
- `service_type` has `CHECK (duration_minutes > 0 AND duration_minutes % 15 = 0)`.

## Done when

`.\gradlew.bat test --tests "com.keyloop.scheduler.SchemaTest" --console=plain` passes.
