# 00: Conventions, global rules, and fixture data

Every story refers to this file. Rules here apply to **all** stories unless a story states otherwise.

## Glossary

| Term | Meaning |
|---|---|
| Dealership | A site with its own opening hours, technicians, and bays |
| Service type | A service a customer can book; defines duration, required skill, and required bay type |
| Technician | A person with one or more skills, working at one dealership; may be inactive |
| Service bay | A physical bay of a given type at one dealership; may be inactive |
| Appointment | A booking of one service type for one vehicle, with one technician and one bay assigned |
| Slot | A candidate `[start, end)` interval for a service type |

## Global rules

- **GR-01 Time.** The system operates in a single time zone: Vietnam time (UTC+07:00, no daylight saving). All dealerships and customers are in Vietnam. Every date and time in the API, the database, and these stories is Vietnam local time. The API uses ISO-8601 local date-times **without an offset**, e.g. `2026-10-12T09:30:00`. A value with an offset or `Z` is invalid (400).
- **GR-02 Intervals.** Appointments are half-open intervals `[start, end)`, where `end = start + serviceType.durationMinutes`. Intervals `[09:00, 10:00)` and `[10:00, 11:00)` do not overlap.
- **GR-03 Grid.** The start time's minutes must be 00, 15, 30, or 45, with seconds and nanoseconds equal to 0.
- **GR-04 Opening hours.** An appointment must lie entirely within the opening hours of its date. A day with no opening hours is closed.
- **GR-05 One service.** Each appointment has exactly one service type. Duration always comes from the service type and is never sent by the client.
- **GR-06 Eligibility.** A technician is *qualified* if they are active, belong to the same dealership, and have the required skill. A bay is *eligible* if it is active, belongs to the same dealership, and has the required bay type.
- **GR-07 Auto-assignment.** Among qualified technicians free for the whole interval, pick the one with the **fewest booked minutes** (CONFIRMED appointments only) on that date, breaking ties by **lowest ID**. Among eligible free bays, pick the **lowest ID**.
- **GR-08 Occupancy.** Only `CONFIRMED` appointments occupy a technician, bay, or vehicle. `CANCELLED` appointments occupy nothing.
- **GR-09 Clock.** All "now" logic uses an injected `java.time.Clock` set to Vietnam time. In tests, the default fixed clock is **`2026-10-11T10:00:00`** (a Sunday) unless an AC states otherwise.
- **GR-10 IDs.** All IDs are UUIDs. A malformed ID returns 400 `VALIDATION_FAILED`. A well-formed but unknown ID returns 404.
- **GR-11 Errors.** Errors use `application/problem+json` (RFC 7807) with these extra fields: `code` (from the catalogue below) and `requestId`. Validation errors add `errors: [{field, message}]`. `detail` is always a human-readable message that a UI can show to the customer as is.
  - 400: the request is malformed.
  - 404: a referenced resource does not exist.
  - 409: the request is valid but conflicts with the current schedule.
  - 422: the request is well-formed but breaks a business rule.
- **GR-12 Privacy.** API responses never include customer email or phone. Logs contain IDs only, never names, emails, phone numbers, or VINs.

## Error code catalogue

| HTTP | code |
|---|---|
| 400 | `VALIDATION_FAILED`, `IDEMPOTENCY_KEY_MISSING` |
| 404 | `CUSTOMER_NOT_FOUND`, `VEHICLE_NOT_FOUND`, `DEALERSHIP_NOT_FOUND`, `SERVICE_TYPE_NOT_FOUND`, `APPOINTMENT_NOT_FOUND` |
| 409 | `NO_QUALIFIED_TECHNICIAN_AVAILABLE`, `NO_SERVICE_BAY_AVAILABLE`, `HIGH_CONTENTION`, `VEHICLE_ALREADY_BOOKED` |
| 422 | `VEHICLE_NOT_OWNED_BY_CUSTOMER`, `START_TIME_IN_PAST`, `START_TIME_NOT_ALIGNED`, `OUTSIDE_OPENING_HOURS`, `IDEMPOTENCY_KEY_REUSED`, `APPOINTMENT_ALREADY_STARTED` |

## Appointment representation

Used by every endpoint that returns an appointment.

```json
{
  "id": "uuid",
  "status": "CONFIRMED | CANCELLED",
  "customer":    { "id": "uuid", "name": "Nguyen Van An" },
  "vehicle":     { "id": "uuid", "vin": "MHFAA8GS0P0000001", "make": "Toyota", "model": "Vios" },
  "dealership":  { "id": "uuid", "name": "Keyloop Motors District 1" },
  "serviceType": { "id": "uuid", "code": "OIL_CHANGE", "name": "Oil Change", "durationMinutes": 30 },
  "technician":  { "id": "uuid", "name": "Tran Minh" },
  "serviceBay":  { "id": "uuid", "name": "Bay 1" },
  "startTime":   "2026-10-12T09:30:00",
  "endTime":     "2026-10-12T10:00:00",
  "createdAt":   "2026-10-11T10:00:00",
  "cancelledAt": null
}
```

## Fixture dataset

These fixed IDs and values are used by the AC, by test setup, and by the `dev` seed data. ID pattern: the first 8 hex digits identify the entity type, and the last digit is the number.

**Dealerships.** Both are open Monday to Saturday 08:00–17:00, closed Sunday.

| Ref | ID | Name |
|---|---|---|
| D1 | `10000000-0000-0000-0000-000000000001` | Keyloop Motors District 1 |
| D2 | `10000000-0000-0000-0000-000000000002` | Keyloop Motors Thu Duc |

**Service types**

| Ref | ID | Code | Name | Duration | Required skill | Required bay type |
|---|---|---|---|---|---|---|
| S1 | `40000000-0000-0000-0000-000000000001` | `OIL_CHANGE` | Oil Change | 30 | `GENERAL_MAINTENANCE` | `GENERAL` |
| S2 | `40000000-0000-0000-0000-000000000002` | `BRAKE_SERVICE` | Brake Service | 90 | `BRAKES` | `GENERAL` |
| S3 | `40000000-0000-0000-0000-000000000003` | `EV_BATTERY_CHECK` | EV Battery Check | 60 | `EV_SYSTEMS` | `EV` |

**Technicians**

| Ref | ID | Name | Dealership | Skills | Active |
|---|---|---|---|---|---|
| T1 | `20000000-0000-0000-0000-000000000001` | Tran Minh | D1 | GENERAL_MAINTENANCE, BRAKES | yes |
| T2 | `20000000-0000-0000-0000-000000000002` | Le Hoa | D1 | GENERAL_MAINTENANCE | yes |
| T3 | `20000000-0000-0000-0000-000000000003` | Pham Duc | D1 | EV_SYSTEMS | yes |
| T4 | `20000000-0000-0000-0000-000000000004` | Vo Lan | D1 | GENERAL_MAINTENANCE | **no** |
| T5 | `20000000-0000-0000-0000-000000000005` | Ngo Bao | D2 | GENERAL_MAINTENANCE | yes |

**Service bays**

| Ref | ID | Name | Dealership | Bay type | Active |
|---|---|---|---|---|---|
| B1 | `30000000-0000-0000-0000-000000000001` | Bay 1 | D1 | GENERAL | yes |
| B2 | `30000000-0000-0000-0000-000000000002` | Bay 2 | D1 | GENERAL | yes |
| B3 | `30000000-0000-0000-0000-000000000003` | EV Bay | D1 | EV | yes |
| B4 | `30000000-0000-0000-0000-000000000004` | Bay 1 | D2 | GENERAL | yes |

**Customers and vehicles**

| Ref | ID | Customer name | Email | Vehicle ref | Vehicle ID | VIN | Make / model |
|---|---|---|---|---|---|---|---|
| C1 | `50000000-0000-0000-0000-000000000001` | Nguyen Van An | an@example.com | V1 | `60000000-0000-0000-0000-000000000001` | MHFAA8GS0P0000001 | Toyota Vios |
| C2 | `50000000-0000-0000-0000-000000000002` | Tran Thi Binh | binh@example.com | V2 | `60000000-0000-0000-0000-000000000002` | MRHGM6660P0000002 | Honda City |
| C3 | `50000000-0000-0000-0000-000000000003` | Le Van Cuong | cuong@example.com | V3 | `60000000-0000-0000-0000-000000000003` | RLLV8E0A0P0000003 | VinFast VF8 |

## How to read the AC

- All times in AC are on **2026-10-12 (Monday)** unless a date is given.
- "T1 has an appointment 09:00–10:30 on B1" means the test inserts a `CONFIRMED` appointment directly, without calling the API. Directly inserted appointments may have any duration. When the AC doesn't name a customer or vehicle, use any fixture customer/vehicle, but overlapping appointments must use **different** vehicles.
- Every booking request in an AC sends a fresh unique `Idempotency-Key` unless the AC states otherwise.
- The database is reset to the fixture state before each test.

## Definition of Done (every story)

1. Every AC has at least one test whose `@DisplayName` starts with the AC ID, and all of them pass.
2. `./gradlew build` passes on a clean clone, using Testcontainers PostgreSQL (no H2 or other embedded database).
3. Frozen files (`docs/user-stories/**` and acceptance tests marked `// FROZEN`) are unchanged.
4. New endpoints appear in the OpenAPI document.
5. The EM has reviewed the change and it is committed as one task per commit.
