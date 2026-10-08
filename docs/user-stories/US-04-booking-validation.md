# US-04: Booking validation

**Priority:** Must · **Depends on:** US-03 · **Status:** Draft

## Story

**As a** vehicle owner
**I want** a clear, specific reason when my booking request cannot be accepted
**So that** I know exactly what to change

## Business rules: check order

Checks run in this order. The **first** failing check determines the response.

1. Request shape: required fields, UUID formats, date-time without an offset (GR-01) → 400 `VALIDATION_FAILED`
2. `Idempotency-Key` header present and non-blank → 400 `IDEMPOTENCY_KEY_MISSING`
3. Existence, in the order customer, vehicle, dealership, service type → 404 (matching code)
4. Vehicle belongs to customer → 422 `VEHICLE_NOT_OWNED_BY_CUSTOMER`
5. Start time strictly after now → 422 `START_TIME_IN_PAST`
6. Start time on the 15-minute grid (GR-03) → 422 `START_TIME_NOT_ALIGNED`
7. Whole interval within opening hours (GR-04) → 422 `OUTSIDE_OPENING_HOURS`
8. Vehicle free (US-03) → 409 `VEHICLE_ALREADY_BOOKED`
9. Capacity (US-03) → 409

## Acceptance criteria

**AC-04.1: Missing field**
When a booking request omits `serviceTypeId`
Then the response is 400 `VALIDATION_FAILED`
And `errors` contains an entry with `field` = `serviceTypeId`

**AC-04.2: Timestamp with an offset**
When `startTime` is `2026-10-12T09:30:00+07:00` or `2026-10-12T02:30:00Z`
Then the response is 400 `VALIDATION_FAILED` with an `errors` entry for `startTime`

**AC-04.3: Malformed JSON**
When the body is not valid JSON
Then the response is 400 `VALIDATION_FAILED`

**AC-04.4: Missing idempotency key**
When an otherwise valid request has no `Idempotency-Key` header
Then the response is 400 `IDEMPOTENCY_KEY_MISSING`

**AC-04.5: Unknown references**

| Unknown field | Response |
|---|---|
| `customerId` | 404 `CUSTOMER_NOT_FOUND` |
| `vehicleId` | 404 `VEHICLE_NOT_FOUND` |
| `dealershipId` | 404 `DEALERSHIP_NOT_FOUND` |
| `serviceTypeId` | 404 `SERVICE_TYPE_NOT_FOUND` |

**AC-04.6: Vehicle not owned by customer**
When C1 books for V2
Then the response is 422 `VEHICLE_NOT_OWNED_BY_CUSTOMER`

**AC-04.7: Start time in the past**
Given the clock is `2026-10-12T09:30:00`
When C1 books OIL_CHANGE starting 09:30
Then the response is 422 `START_TIME_IN_PAST`
When C1 books OIL_CHANGE starting 09:45
Then the response is 201

**AC-04.8: Start time not on the grid**
When `startTime` is `2026-10-12T09:10:00` or `2026-10-12T09:15:30`
Then the response is 422 `START_TIME_NOT_ALIGNED`

**AC-04.9: Opening hours**

| Service | Start | Response |
|---|---|---|
| OIL_CHANGE | 2026-10-12 07:45 | 422 `OUTSIDE_OPENING_HOURS` |
| OIL_CHANGE | 2026-10-12 16:45 | 422 `OUTSIDE_OPENING_HOURS` |
| BRAKE_SERVICE | 2026-10-12 16:00 | 422 `OUTSIDE_OPENING_HOURS` |
| OIL_CHANGE | 2026-10-18 10:00 (Sunday) | 422 `OUTSIDE_OPENING_HOURS` |
| OIL_CHANGE | 2026-10-12 08:00 | 201 |
| OIL_CHANGE | 2026-10-12 16:30 | 201 |

**AC-04.10: Check order**
When `startTime` is `2026-10-10T09:10:00` (both in the past and off the grid)
Then the response is 422 `START_TIME_IN_PAST`

**AC-04.11: No side effects on rejection**
Given any rejected request in AC-04.1 to AC-04.10
Then the number of stored appointments is unchanged

## Notes for implementer

- Use Bean Validation for checks 1–2 and a single domain validator, in the order above, for checks 3–7.
