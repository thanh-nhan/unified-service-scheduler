# US-06: View an appointment

**Priority:** Must · **Depends on:** US-03 · **Status:** Draft

## Story

**As a** vehicle owner
**I want to** view my appointment's details
**So that** I know when to arrive and what was booked

## API

`GET /api/v1/appointments/{id}` returns 200 with the appointment representation.

## Acceptance criteria

**AC-06.1: View a confirmed appointment**
Given C1 booked OIL_CHANGE for V1 at 09:30 and received appointment id `A`
When the client calls `GET /api/v1/appointments/A`
Then the response is 200 with the same fields and values as the booking response
And `startTime` is `2026-10-12T09:30:00+07:00`

**AC-06.2: View a cancelled appointment**
Given appointment `A` has been cancelled
When the client calls `GET /api/v1/appointments/A`
Then the response is 200 with status CANCELLED and a non-null `cancelledAt`

**AC-06.3: Unknown appointment**
When the client requests a well-formed but unknown id
Then the response is 404 `APPOINTMENT_NOT_FOUND`

**AC-06.4: Malformed id**
When the client calls `GET /api/v1/appointments/abc`
Then the response is 400 `VALIDATION_FAILED`

**AC-06.5: No contact details exposed**
When the client views any appointment
Then the response contains no customer email or phone number
