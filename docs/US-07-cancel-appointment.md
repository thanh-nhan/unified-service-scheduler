# US-07: Cancel an appointment

**Priority:** Must · **Depends on:** US-06 · **Status:** Draft

## Story

**As a** vehicle owner
**I want to** cancel an appointment I no longer need
**So that** the time is freed for other customers and I am not expected at the dealership

## API

`POST /api/v1/appointments/{id}/cancel` (no body) returns 200 with the appointment representation.

## Business rules

- Only appointments that have not started yet (`startTime` strictly after now) can be cancelled.
- Cancelling is idempotent. Cancelling an already-cancelled appointment returns 200 and changes nothing.
- A cancelled appointment immediately frees its technician, bay, and vehicle (GR-08).

## Acceptance criteria

**AC-07.1: Cancel a future appointment**
Given appointment `A` is CONFIRMED for 2026-10-12 09:30
When the client cancels `A`
Then the response is 200 with status CANCELLED and `cancelledAt` = `2026-10-11T10:00:00+07:00`

**AC-07.2: Cancellation frees resources**
Given C3 has appointment `A` for EV_BATTERY_CHECK at 09:00 with T3 on B3
And C3 cancels `A`
When C3 books EV_BATTERY_CHECK for V3 at 09:00 again
Then the response is 201 with technician T3 and bay B3

**AC-07.3: Availability reflects the cancellation**
Given T3's only appointment (EV, 09:00–10:00) is cancelled
When the client requests availability for D1, EV_BATTERY_CHECK, 2026-10-12
Then the 09:00 slot is included

**AC-07.4: Cancelling twice**
Given `A` was cancelled at `2026-10-11T10:00:00+07:00`
And the clock then moves to `2026-10-11T11:00:00+07:00`
When the client cancels `A` again
Then the response is 200 with `cancelledAt` still `2026-10-11T10:00:00+07:00`

**AC-07.5: Started appointments cannot be cancelled**
Given the clock is `2026-10-12T09:00:00+07:00`
And `A` is CONFIRMED starting at 09:00
When the client cancels `A`
Then the response is 422 `APPOINTMENT_ALREADY_STARTED`
And `A` remains CONFIRMED

**AC-07.6: Unknown appointment**
When the client cancels a well-formed but unknown id
Then the response is 404 `APPOINTMENT_NOT_FOUND`

**AC-07.7: Concurrent cancellations**
When two cancel requests for `A` are sent concurrently
Then both responses are 200 with status CANCELLED and the same `cancelledAt`
And no response is 5xx
