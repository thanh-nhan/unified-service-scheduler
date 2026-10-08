# US-09: View weekly availability

**Priority:** Must · **Depends on:** US-02 · **Status:** Draft

## Story

**As a** vehicle owner
**I want to** see every available start time for my chosen service at a dealership across a whole week, Monday to Sunday
**So that** I can compare days at a glance and pick the time that suits me best

## API

`GET /api/v1/dealerships/{dealershipId}/availability/weekly?serviceTypeId={uuid}&weekStart={yyyy-MM-dd}`

- `weekStart` is optional. It must be a **Monday**. If it is omitted, the week is the one containing today (per the `Clock`), starting on that week's Monday.

```json
{
  "dealershipId": "uuid", "serviceTypeId": "uuid", "durationMinutes": 30,
  "weekStart": "2026-10-12", "weekEnd": "2026-10-18",
  "days": [
    { "date": "2026-10-12", "dayOfWeek": "MONDAY", "open": true,
      "slots": [ { "startTime": "2026-10-12T08:00:00", "endTime": "2026-10-12T08:30:00" } ] },
    { "date": "2026-10-18", "dayOfWeek": "SUNDAY", "open": false, "slots": [] }
  ]
}
```

## Business rules

- `days` always has exactly 7 entries, Monday to Sunday, in date order, so a UI can render a full week grid.
- `open` is `true` if the dealership has opening hours on that weekday (GR-04), otherwise `false`. A closed day always has `slots: []`.
- Each day's `slots` follow exactly the US-02 rules: 15-minute grid, a qualified technician **and** an eligible bay free for the whole interval, within opening hours, and strictly after "now". Past days are still `open: true` but have `slots: []`.
- Slots within each day are sorted by `startTime` ascending.
- Like US-02, this is a snapshot, not a reservation. Booking (US-03) re-checks everything.

## Acceptance criteria

**AC-09.1: Full week with no appointments**
Given no appointments
When the client requests weekly availability for D1, OIL_CHANGE, `weekStart=2026-10-12`
Then the response is 200 with `weekStart` 2026-10-12, `weekEnd` 2026-10-18, and `durationMinutes` 30
And `days` has 7 entries, MONDAY 2026-10-12 to SUNDAY 2026-10-18, in that order
And MONDAY to SATURDAY each have `open: true` and exactly 35 slots, the first 08:00–08:30 and the last 16:30–17:00
And SUNDAY has `open: false` and `slots: []`

**AC-09.2: Default week, with past times excluded**
Given the clock is `2026-10-14T10:05:00` (a Wednesday)
When the client requests weekly availability for D1, OIL_CHANGE without `weekStart`
Then the response has `weekStart` 2026-10-12 and `weekEnd` 2026-10-18
And MONDAY and TUESDAY have `open: true` and `slots: []`
And WEDNESDAY has 26 slots, the first 10:15–10:45 and the last 16:30–17:00
And THURSDAY, FRIDAY, and SATURDAY each have 35 slots

**AC-09.3: Appointments only affect their own day**
Given T1 has an appointment `2026-10-13T09:00:00`–`2026-10-13T10:30:00` on B1
When the client requests weekly availability for D1, BRAKE_SERVICE, `weekStart=2026-10-12`
Then TUESDAY has exactly 21 slots and its first slot is 10:30–12:00
And MONDAY, WEDNESDAY, THURSDAY, FRIDAY, and SATURDAY each have exactly 31 slots

**AC-09.4: Same result as the daily view**
Given T1 has an appointment `2026-10-13T09:00:00`–`2026-10-13T10:30:00` on B1
And T2 has an appointment `2026-10-15T14:00:00`–`2026-10-15T15:00:00` on B2
When the client requests weekly availability for D1, OIL_CHANGE, `weekStart=2026-10-12`
Then for each of the 7 days, that day's `slots` are identical to the `slots` returned by US-02 for the same dealership, service type, and date

**AC-09.5: Dealership without the capability**
When the client requests weekly availability for D2, EV_BATTERY_CHECK, `weekStart=2026-10-12`
Then the response is 200 with 7 days
And MONDAY to SATURDAY have `open: true` and `slots: []`

**AC-09.6: Invalid requests**

| Request | Response |
|---|---|
| `weekStart=2026-10-14` (a Wednesday) | 400 `VALIDATION_FAILED`, with an `errors` entry for `weekStart` |
| `weekStart=14-10-2026` | 400 `VALIDATION_FAILED` |
| Missing `serviceTypeId` | 400 `VALIDATION_FAILED` |
| Unknown `dealershipId` | 404 `DEALERSHIP_NOT_FOUND` |
| Unknown `serviceTypeId` | 404 `SERVICE_TYPE_NOT_FOUND` |

## Notes for implementer

- Reuse the US-02 `AvailabilityCalculator` for each day. Do **not** write a second slot or overlap calculation.
- Load the week's appointments and resources in a fixed number of queries, then calculate all 7 days in memory. Never call the database once per day or once per slot.
