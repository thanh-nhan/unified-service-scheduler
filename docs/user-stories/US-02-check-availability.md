# US-02: Check availability

**Priority:** Must · **Depends on:** US-01 · **Status:** Draft

## Story

**As a** vehicle owner
**I want to** see which start times are available for a service at a dealership on a given day
**So that** I only try to book times that can actually be fulfilled

## API

`GET /api/v1/dealerships/{dealershipId}/availability?serviceTypeId={uuid}&date={yyyy-MM-dd}`

`date` is a calendar date (Vietnam time, GR-01).

```json
{
  "dealershipId": "uuid", "serviceTypeId": "uuid", "date": "2026-10-12",
  "durationMinutes": 30,
  "slots": [ { "startTime": "2026-10-12T08:00:00", "endTime": "2026-10-12T08:30:00" } ]
}
```

## Business rules

- A start time `s` (on the 15-minute grid, GR-03) is available if, for `[s, s + duration)`, at least one qualified technician **and** at least one eligible bay are free (GR-06, GR-08), and the interval fits within opening hours (GR-04).
- Start times at or before "now" are excluded.
- Slots are sorted by `startTime` ascending.
- Availability is a snapshot, not a reservation. Booking re-checks everything.
- This endpoint is how customers choose a start time. The booking API does not suggest alternatives (see US-03).

## Acceptance criteria

**AC-02.1: Full day with no appointments**
Given no appointments
When the client requests availability for D1, OIL_CHANGE, 2026-10-12
Then the response is 200 with `durationMinutes` 30
And there are exactly 35 slots, the first 08:00–08:30 and the last 16:30–17:00

**AC-02.2: Only qualified technician is busy**
Given T1 has an appointment 09:00–10:30 on B1
When the client requests availability for D1, BRAKE_SERVICE, 2026-10-12
Then no slot starts between 08:00 and 10:15 inclusive
And the first slot is 10:30–12:00
And there are exactly 21 slots

**AC-02.3: Other resources keep a slot open**
Given T1 has an appointment 09:00–09:30 on B1
When the client requests availability for D1, OIL_CHANGE, 2026-10-12
Then the 09:00 slot is included (T2 and B2 are free)

**AC-02.4: All qualified technicians busy**
Given T1 has an appointment 09:00–09:30 on B1
And T2 has an appointment 09:00–09:30 on B2
When the client requests availability for D1, OIL_CHANGE, 2026-10-12
Then slots starting at 08:45, 09:00, and 09:15 are **not** included
And slots starting at 08:30 and 09:30 **are** included

**AC-02.5: Closed day**
When the client requests availability for D1, OIL_CHANGE, 2026-10-18 (a Sunday)
Then the response is 200 with `slots: []`

**AC-02.6: Past start times excluded today**
Given the clock is `2026-10-12T10:05:00`
When the client requests availability for D1, OIL_CHANGE, 2026-10-12
Then the first slot is 10:15–10:45

**AC-02.7: Past date**
Given the default clock (`2026-10-11T10:00:00`)
When the client requests availability for D1, OIL_CHANGE, 2026-10-10
Then the response is 200 with `slots: []`

**AC-02.8: Dealership without the capability**
When the client requests availability for D2, EV_BATTERY_CHECK, 2026-10-12
Then the response is 200 with `slots: []`

**AC-02.9: Cancelled, inactive, and other-dealership resources ignored**
Given T1 and T2 each have a CANCELLED appointment 09:00–09:30
Then the OIL_CHANGE 09:00 slot at D1 is included
Given T1 and T2 each have a CONFIRMED appointment 09:00–09:30
Then the OIL_CHANGE 09:00 slot at D1 is **not** included, even though T4 (inactive) and T5 (at D2) are free

**AC-02.10: Invalid requests**

| Request | Response |
|---|---|
| Unknown `dealershipId` | 404 `DEALERSHIP_NOT_FOUND` |
| Unknown `serviceTypeId` | 404 `SERVICE_TYPE_NOT_FOUND` |
| Missing `serviceTypeId` or `date` | 400 `VALIDATION_FAILED` |
| `date=12-10-2026` | 400 `VALIDATION_FAILED` |

## Notes for implementer

- Put the calculation in one component (e.g. `AvailabilityCalculator`) with no web or database code, so it can be unit tested in isolation. US-09 (weekly availability) reuses it for each day.
- Load appointments for the whole day in a fixed number of queries. Never query the database once per slot.
