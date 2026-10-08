# US-08: List my appointments

**Priority:** Should · **Depends on:** US-06 · **Status:** Draft

## Story

**As a** vehicle owner
**I want to** see all my appointments in one list
**So that** I can keep track of upcoming and past services

## API

`GET /api/v1/customers/{customerId}/appointments?status={CONFIRMED|CANCELLED}&page={n}&size={n}`

```json
{ "items": [ /* appointment representations */ ],
  "page": 0, "size": 20, "totalItems": 2, "totalPages": 1 }
```

- Defaults: no status filter (all statuses, past and future), `page=0`, `size=20`.
- Limits: `page >= 0` and `1 <= size <= 100`.
- Items are sorted by `startTime` ascending, then `id` ascending.

## Acceptance criteria

**AC-08.1: Only the customer's own appointments**
Given C1 has appointments at 11:00 and 09:00, and C2 has one at 10:00
When the client lists C1's appointments
Then the response is 200 with exactly 2 items, in the order 09:00, 11:00

**AC-08.2: Status filter**
Given C1 has one CONFIRMED and one CANCELLED appointment
When the client lists C1's appointments with `status=CANCELLED`
Then exactly 1 item is returned, and it is CANCELLED

**AC-08.3: Pagination**
Given C1 has 25 non-overlapping appointments
When the client lists with `page=2&size=10`
Then 5 items are returned, with `totalItems` 25 and `totalPages` 3

**AC-08.4: Invalid paging parameters**
When `size=101`, `size=0`, or `page=-1` is sent
Then the response is 400 `VALIDATION_FAILED`

**AC-08.5: Unknown or empty customer**
When the client lists appointments for an unknown customer id
Then the response is 404 `CUSTOMER_NOT_FOUND`
When the client lists appointments for C2 with no appointments
Then the response is 200 with `items: []` and `totalItems` 0
