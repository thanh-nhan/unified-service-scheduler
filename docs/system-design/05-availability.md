# 05: Availability

Covers US-02 (one day) and US-09 (one week, Monday to Sunday).

## 1. Definitions

- **Candidate start times:** every 15 minutes from `opensAt` while `start + duration <= closesAt` (GR-03, GR-04).
- **Free:** a resource is free for `[s, e)` if none of its CONFIRMED appointments overlap it (`a.start < e && a.end > s`, GR-02).
- **Available slot:** a candidate start time `s` that is strictly after now, where at least one qualified technician **and** at least one eligible bay are free for `[s, s + duration)`.

Checking technicians and bays independently is correct because any free qualified technician can work in any free eligible bay at the same dealership.

## 2. The calculator (pure)

```java
// availability/AvailabilityCalculator.java: no Spring, no database, no clock access
List<Slot> calculate(LocalDate date,
                     OpeningHours hours,                // null = closed
                     int durationMinutes,
                     Set<UUID> technicianIds,           // qualified, active, same dealership
                     Set<UUID> bayIds,                  // eligible, active, same dealership
                     Map<UUID, List<BusyInterval>> busy, // CONFIRMED intervals per resource id
                     LocalDateTime now)
```

```text
if hours == null: return []
slots = []
for s = date.atTime(hours.opensAt); s + duration <= date.atTime(hours.closesAt); s += 15 min:
    if not s.isAfter(now): continue
    e = s + duration
    techFree = any t in technicianIds where no interval in busy[t] overlaps [s, e)
    bayFree  = any b in bayIds        where no interval in busy[b] overlaps [s, e)
    if techFree and bayFree: slots.add(Slot(s, e))
return slots   // already in ascending order
```

The calculator is the single implementation of "is this start time available". Its unit tests cover the overlap edge cases (back-to-back, contained, containing, partial), past times, closed days, and empty resource sets.

## 3. AvailabilityService: loading data

Both endpoints load data for a date range `[from, to)` in a **fixed number of queries**, independent of the number of days or slots:

1. Dealership with opening hours, and service type (404 if missing).
2. Qualified technician ids: active, same dealership, with the required skill.
3. Eligible bay ids: active, same dealership, with the required bay type.
4. CONFIRMED appointments overlapping `[from, to)` whose technician is in (2) **or** bay is in (3): `technician_id, service_bay_id, start_time, end_time`. Uses `ix_appointment_dealership_start`.

The service groups (4) into busy intervals per technician id and per bay id, then calls the calculator once per day.

| Endpoint | Range | Calculator calls |
|---|---|---|
| Daily (US-02) | `[date 00:00, date+1 00:00)` | 1 |
| Weekly (US-09) | `[weekStart 00:00, weekStart+7 00:00)` | 7, one per day Monday to Sunday |

The weekly response always has 7 entries. `open = (opening hours exist for that weekday)`. A closed day has `slots: []` without calling the calculator. Because both endpoints use the same calculator with the same inputs, each weekly day equals the daily result for that date (AC-09.4).

## 4. Validation

| Input | Rule | Error |
|---|---|---|
| `date`, `weekStart` | ISO `yyyy-MM-dd` | 400 `VALIDATION_FAILED` |
| `weekStart` | Must be a Monday (`getDayOfWeek() == MONDAY`) | 400 `VALIDATION_FAILED`, `errors[].field = weekStart` |
| `weekStart` omitted | `LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(MONDAY))` | n/a |
| `serviceTypeId` | Required UUID | 400 `VALIDATION_FAILED` |
| Unknown dealership or service type | n/a | 404 `DEALERSHIP_NOT_FOUND` / `SERVICE_TYPE_NOT_FOUND` |

Past dates and dealerships without the needed resources return 200 with empty slots, not errors.

## 5. Cost

For one dealership and one week: about 10 technicians, 8 bays, and at most a few hundred appointments, loaded by 4 indexed queries. In memory: 7 days × up to 36 candidates × 18 resources × a handful of intervals, which is a few tens of thousands of comparisons, well under a millisecond. Database round trips dominate latency.

## 6. Consistency

Availability is a snapshot, not a reservation. Between reading slots and booking, another customer may take the slot. Booking re-validates everything and returns 409 with a readable message; the UI then refreshes availability (ADR-006). This is the expected, rare path, not an error condition.
