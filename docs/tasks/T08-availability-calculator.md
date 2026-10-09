# T08: Scheduling rules and availability calculator (pure)

**Prepares:** US-02, US-09, US-04 · **Type:** single task (unit tests written in the same task)

## Goal

The single implementation of "is this start time available", as pure Java with exhaustive unit tests. No Spring, no database, no clock.

## Read first

- `docs/system-design/05-availability.md` §1, §2
- `docs/user-stories/00-conventions.md` (GR-02, GR-03, GR-04 only)

## Files

All under `availability/` in main, and the matching test package:

| Action | File |
|---|---|
| Create | `Slot.java` (record: `LocalDateTime start`, `LocalDateTime end`) |
| Create | `BusyInterval.java` (record: `LocalDateTime start`, `LocalDateTime end`; method `boolean overlaps(LocalDateTime s, LocalDateTime e)`) |
| Create | `DailyHours.java` (record: `LocalTime opensAt`, `LocalTime closesAt`) |
| Create | `SchedulingRules.java` |
| Create | `AvailabilityCalculator.java` |
| Create | `SchedulingRulesTest.java`, `AvailabilityCalculatorTest.java` |

## Specification

**`SchedulingRules`** (static methods):
- `boolean isOnGrid(LocalDateTime t)`: minute in {0, 15, 30, 45}, second and nano 0.
- `boolean fitsOpeningHours(LocalDateTime start, LocalDateTime end, DailyHours hours)`: `hours != null`, same date, `start >= opensAt`, `end <= closesAt`.

**`BusyInterval.overlaps`:** half-open, `start < e && end > s`.

**`AvailabilityCalculator.calculate(...)`:** exactly the signature and pseudocode in 05 §2, using `DailyHours` (null = closed), `Set<UUID> technicianIds`, `Set<UUID> bayIds`, `Map<UUID, List<BusyInterval>> busy`, and `LocalDateTime now`. Returns slots in ascending order.

## Unit tests (required cases)

`BusyInterval.overlaps` for existing `[09:00, 10:00)`:

| Candidate | Expected |
|---|---|
| `[09:30, 10:00)` | true |
| `[10:00, 11:00)` | false |
| `[08:00, 09:00)` | false |
| `[08:30, 11:00)` | true |
| `[09:15, 09:45)` | true |

Calculator (D1 hours 08:00–17:00, 30-minute service, 2 technicians, 2 bays, now = previous day):
- No busy intervals → 35 slots, first 08:00, last 16:30.
- 90-minute service, the only technician busy 09:00–10:30 → 21 slots, first 10:30.
- Both technicians busy 09:00–09:30 → no slots starting 08:45, 09:00, 09:15; slots at 08:30 and 09:30 exist.
- One technician busy, other free → slot present.
- Technicians free but all bays busy → slot absent.
- `now` = same day 10:05 → first slot 10:15.
- `hours == null` → empty. Empty technician set → empty.

`SchedulingRules`: grid true for 09:00/09:15/09:45; false for 09:10 and 09:15:30. Opening hours: 08:00–08:30 fits; 07:45 start, 17:15 end, and null hours do not.

## Watch for (EM)

- No Spring annotations, no repositories, no `now()` calls in these classes.
- The loop condition is `start + duration <= closesAt` (inclusive end).

## Done when

`.\gradlew.bat test --tests "com.keyloop.scheduler.availability.*" --console=plain` passes.
