# US-03: Book an appointment

**Priority:** Must · **Depends on:** US-02 · **Status:** Draft

## Story

**As a** vehicle owner
**I want to** book a service for my vehicle at a dealership at a time that suits me
**So that** my car is serviced without phoning the dealership

## API

`POST /api/v1/appointments` with header `Idempotency-Key: <string>`

```json
{ "customerId": "uuid", "vehicleId": "uuid", "dealershipId": "uuid",
  "serviceTypeId": "uuid", "startTime": "2026-10-12T09:30:00+07:00" }
```

On success: 201 Created, header `Location: /api/v1/appointments/{id}`, body = appointment representation (see `00-conventions.md`).

## Business rules

- The system assigns the technician and bay automatically (GR-07). The client cannot choose them.
- A booking is all-or-nothing. If either resource is unavailable, nothing is created.
- A vehicle cannot have two overlapping CONFIRMED appointments.
- On a capacity conflict, the 409 body contains `alternatives`: up to 3 available slots on the requested day, closest to the requested start first (ties: earlier first). If the requested day has none, use the first 3 slots of the next open day within 7 days. Return `[]` if none are found.
- Validation rules and their order are in US-04. Concurrency and idempotency are in US-05.

## Acceptance criteria

**AC-03.1: Successful booking**
Given no appointments
When C1 books OIL_CHANGE for V1 at D1 starting `2026-10-12T09:30:00+07:00`
Then the response is 201 with a `Location` header for the new appointment
And the body has status CONFIRMED, start 09:30, end 10:00, technician T1, and bay B1
And exactly 1 appointment is stored

**AC-03.2: Busy technician is avoided**
Given T1 has an appointment 09:00–10:30 on B1
When C2 books OIL_CHANGE for V2 starting 09:30
Then the response is 201 with technician T2 and bay B2

**AC-03.3: Least-loaded technician is chosen**
Given T1 has an appointment 08:00–08:30 on B1
When C2 books OIL_CHANGE for V2 starting 11:00
Then the response is 201 with technician T2 and bay B1

**AC-03.4: Required skill beats workload**
Given T1 has an appointment 08:00–08:30 on B1
When C2 books BRAKE_SERVICE for V2 starting 11:00
Then the response is 201 with technician T1 (the only technician with BRAKES), even though T2 has fewer booked minutes

**AC-03.5: No qualified technician, with alternatives**
Given T1 has an appointment 09:00–10:30 on B1
When C2 books BRAKE_SERVICE for V2 starting 09:30
Then the response is 409 with code `NO_QUALIFIED_TECHNICIAN_AVAILABLE`
And `alternatives` is exactly [10:30–12:00, 10:45–12:15, 11:00–12:30]
And no new appointment is stored

**AC-03.6: No bay available**
Given an extra active technician T6 at D1 with skill GENERAL_MAINTENANCE
And T1 has an appointment 09:30–10:00 on B1
And T2 has an appointment 09:30–10:00 on B2
When C1 books OIL_CHANGE for V1 starting 09:30
Then the response is 409 with code `NO_SERVICE_BAY_AVAILABLE`
And no new appointment is stored

**AC-03.7: Back-to-back is allowed**
Given T1 has an appointment 08:30–10:00 on B1
When C2 books BRAKE_SERVICE for V2 starting 10:00
Then the response is 201 with technician T1 and bay B1

**AC-03.8: Inactive and other-dealership resources are never used**
Given T1 has an appointment 09:00–10:30 on B1
And T2 has an appointment 09:00–10:30 on B2
When C1 books OIL_CHANGE for V1 at D1 starting 09:30
Then the response is 409 with code `NO_QUALIFIED_TECHNICIAN_AVAILABLE`, even though T4 (inactive) and T5 (D2) are free

**AC-03.9: Bay type is respected**
When C3 books EV_BATTERY_CHECK for V3 starting 14:00
Then the response is 201 with technician T3 and bay B3

**AC-03.10: Vehicle cannot be double-booked**
Given V1 has an appointment 09:00–09:30 with T2 on B2
When C1 books BRAKE_SERVICE for V1 starting 09:15
Then the response is 409 with code `VEHICLE_ALREADY_BOOKED` and no `alternatives` field
And no new appointment is stored

**AC-03.11: Alternatives fall back to the next open day**
Given T1 has an appointment 08:00–17:00 on 2026-10-12 on B1
When C2 books BRAKE_SERVICE for V2 starting `2026-10-12T09:00:00+07:00`
Then the response is 409 with `alternatives` exactly [2026-10-13 08:00–09:30, 08:15–09:45, 08:30–10:00]

## Notes for implementer

- Build alternatives with the US-02 availability component. Do not write a second overlap calculation.
- Correctness under concurrent requests is specified in US-05. Design the booking flow so that the database, not only application code, prevents overlaps (see the approved design / ADR).
