# US-01: Browse dealerships and service types

**Priority:** Must · **Depends on:** E-01 · **Status:** Draft

## Story

**As a** vehicle owner
**I want to** see the dealerships and the services I can book, with their durations
**So that** I can decide what and where to book

## API

- `GET /api/v1/dealerships` returns a JSON array sorted by `name` ascending:
  `[{ "id", "name", "openingHours": [{ "dayOfWeek": "MONDAY", "opensAt": "08:00", "closesAt": "17:00" }] }]`
  `openingHours` is ordered MONDAY to SUNDAY, and closed days are omitted.
- `GET /api/v1/service-types` returns a JSON array sorted by `name` ascending:
  `[{ "id", "code", "name", "durationMinutes" }]`
  Required skill and bay type are internal and **not** exposed.

## Acceptance criteria

**AC-01.1: List dealerships**
Given the fixture dataset
When the client calls `GET /api/v1/dealerships`
Then the response is 200 with 2 items in the order D1, D2
And D1 has 6 opening-hours entries, MONDAY to SATURDAY, each 08:00–17:00

**AC-01.2: List service types**
Given the fixture dataset
When the client calls `GET /api/v1/service-types`
Then the response is 200 with 3 items in the order Brake Service (90), EV Battery Check (60), Oil Change (30)
And no item contains a required skill or a bay type

**AC-01.3: Empty catalogue**
Given there are no service types
When the client calls `GET /api/v1/service-types`
Then the response is 200 with `[]`

## Out of scope

Creating or editing dealerships or service types, and filtering services by dealership (see PO decision 6).
