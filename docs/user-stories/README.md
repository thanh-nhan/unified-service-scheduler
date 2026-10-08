# User Stories: Unified Service Scheduler

**Status:** DRAFT, pending PO approval · **Version:** 0.1 · **Date:** 2026-10-08

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
| 3 | US-02 | Check availability | Must | US-01 |
| 4 | US-03 | Book an appointment | Must | US-02 |
| 5 | US-04 | Booking validation | Must | US-03 |
| 6 | US-05 | Reliable booking under concurrency and retries | Must | US-03 |
| 7 | US-06 | View an appointment | Must | US-03 |
| 8 | US-07 | Cancel an appointment | Must | US-06 |
| 9 | E-02 | Observability (enabler) | Must | US-03 |
| 10 | E-03 | API documentation and client harness (enabler) | Must | US-07 |
| 11 | US-08 | List my appointments | Should | US-06 |

Availability (US-02) comes before booking because the booking's 409 response reuses the availability calculation to suggest alternatives.

## Decisions requiring PO confirmation

Each item below is drafted with the stated assumption. Confirm or change it before approval.

1. **Vehicle double-booking.** A vehicle cannot have two overlapping confirmed appointments (AC-03.10). *Assumed: yes.*
2. **Cancellation cut-off.** Customers can cancel any time before the appointment starts, with no fee (US-07). *Assumed: no earlier cut-off.*
3. **Alternatives on conflict.** Up to 3 slots closest to the requested time on the same day; if none, the first 3 slots of the next open day within 7 days (AC-03.5, AC-03.11).
4. **Slot grid.** Start times on a 15-minute grid (GR-03).
5. **Booking horizon.** No maximum days-ahead limit in the MVP.
6. **Service catalogue.** Service types are global. A dealership without a qualified technician or suitable bay simply has no availability for that service (AC-02.8).
7. **Confirmation details.** The customer sees the assigned technician's name and bay name.

## Out of scope (MVP)

Authentication (`customerId` is sent in the request; in production it would come from a JWT), rescheduling, multiple services per appointment, technician shifts, breaks and leave, public holidays, buffer time between appointments, notifications, payments, and admin screens for managing resources (seed data only).

## Change log

| Version | Date | Change | Approved by |
|---|---|---|---|
| 0.1 | 2026-10-08 | Initial draft | pending |
