# ADR-006: No alternative-slot suggestions on conflict

**Status:** Accepted by PO (user stories v0.2) · **Date:** 2026-10-08

## Context

When a requested time cannot be booked, the API could suggest nearby available times. The first draft returned up to three alternatives, with a next-day fallback. The future UI will let customers pick start times from the availability view (US-02, US-09).

## Decision

A booking conflict returns 409 with a specific `code` and a human-readable `detail`, and **no** alternatives. The UI shows the message and refreshes availability.

## Consequences

- One source of truth for availability (the availability endpoints); no second slot-search algorithm in booking.
- Smaller booking response and fewer test cases.
- The UI must refresh availability after a 409, which it needs to do anyway because the customer's view is stale.

## Alternatives considered

| Option | Why not |
|---|---|
| Return up to 3 nearby slots | Duplicates availability logic and widens the contract for a UX need already met by the weekly view |
