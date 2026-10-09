# User Stories: Unified Service Scheduler

**Status:** APPROVED · **Version:** 0.3 · **Date:** 2026-10-08

| Role | Responsibility |
|---|---|
| Product Owner | Approves stories and acceptance criteria (AC) |
| Engineering Manager | Approves design, task breakdown, and code reviews |
| Planning AI (Claude) | Drafts stories, design, and tasks |
| Coding AI (qwen3-coder:30b via Cline + Ollama) | Implements tasks against approved AC |

## How these files are used

- **Contract.** Once approved, story files are frozen. Any change needs PO/EM approval and a new entry in the change log below.
- **Coding sessions.** Each Cline task starts fresh, with project rules from `.clinerules/`. The task prompt tells the model to read `00-conventions.md` and exactly **one** story file, and never to edit anything under `docs/`. Files are kept small to fit a 32k context window alongside the code.
- **Traceability.** Every AC has an ID (e.g. `AC-03.2`). The test that verifies it puts that ID at the start of its `@DisplayName`. Running `grep -rn "AC-" src/test` produces the traceability matrix.

## Backlog (implementation order)

| # | ID | Title | Priority | Depends on |
|---|---|---|---|---|
| 1 | E-01 | Project foundation (enabler) | Must | none |
| 2 | US-01 | Browse dealerships and service types | Must | E-01 |
| 3 | US-02 | Check availability (one day) | Must | US-01 |
| 4 | US-09 | View weekly availability | Must | US-02 |
| 5 | US-03 | Book an appointment | Must | US-02 |
| 6 | US-04 | Booking validation | Must | US-03 |
| 7 | US-05 | Reliable booking under concurrency and retries | Must | US-03 |
| 8 | US-06 | View an appointment | Must | US-03 |
| 9 | US-07 | Cancel an appointment | Must | US-06 |
| 10 | E-02 | Observability (enabler) | Must | US-03 |
| 11 | E-03 | API documentation and client harness (enabler) | Must | US-07 |
| 12 | US-08 | List my appointments | Should | US-06 |

Availability (US-02) comes before booking because customers pick a start time from the available slots, usually from the weekly view (US-09). If the chosen slot is taken in the meantime, booking returns 409 and the customer picks another slot. The API does not suggest alternatives.

## Decisions requiring PO confirmation

Each item below is drafted with the stated assumption. Confirm or change it before approval.

1. **Vehicle double-booking.** A vehicle cannot have two overlapping confirmed appointments (AC-03.10). *Assumed: yes.*
2. **Cancellation cut-off.** Customers can cancel any time before the appointment starts, with no fee (US-07). *Assumed: no earlier cut-off.*
3. **Slot grid.** Start times on a 15-minute grid (GR-03).
4. **Booking horizon.** No maximum days-ahead limit in the MVP.
5. **Service catalogue.** Service types are global. A dealership without a qualified technician or suitable bay simply has no availability for that service (AC-02.8).
6. **Confirmation details.** The customer sees the assigned technician's name and bay name.

## Out of scope (MVP)

Multiple time zones (Vietnam time only), alternative-slot suggestions on conflict (the UI uses US-02 and US-09 instead), authentication (`customerId` is sent in the request; in production it would come from a JWT), rescheduling, multiple services per appointment, technician shifts, breaks and leave, public holidays, buffer time between appointments, notifications, payments, and admin screens for managing resources (seed data only).

## Change log

| Version | Date | Change | Approved by |
|---|---|---|---|
| 0.1 | 2026-10-08 | Initial draft | n/a |
| 0.2 | 2026-10-08 | PO review: removed alternative slots from the 409 response (US-03, US-05, E-03). Single time zone (Vietnam): offsets and dealership timezone removed from all stories | n/a |
| 0.3 | 2026-10-08 | Added US-09 (weekly availability, Monday to Sunday). US-09 keeps its ID; it is implemented right after US-02. PO decisions: keep US-02 (single day) alongside US-09; `weekStart` must be a Monday, and any other date is rejected with 400 (AC-09.6) | PO, 2026-10-08 |
