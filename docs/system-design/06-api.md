# 06: API

The user stories define each endpoint's exact request, response, and error behavior. This document lists the surface and the cross-cutting conventions.

## 1. Endpoints

Base path: `/api/v1`.

| Method | Path | Story | Success |
|---|---|---|---|
| GET | `/dealerships` | US-01 | 200 |
| GET | `/service-types` | US-01 | 200 |
| GET | `/dealerships/{dealershipId}/availability?serviceTypeId&date` | US-02 | 200 |
| GET | `/dealerships/{dealershipId}/availability/weekly?serviceTypeId&weekStart` | US-09 | 200 |
| POST | `/appointments` (header `Idempotency-Key`) | US-03, US-04, US-05 | 201 (new), 200 (replay) |
| GET | `/appointments/{id}` | US-06 | 200 |
| POST | `/appointments/{id}/cancel` | US-07 | 200 |
| GET | `/customers/{customerId}/appointments?status&page&size` | US-08 | 200 |

Operational endpoints (not versioned): `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`, `/actuator/prometheus`, `/v3/api-docs`, `/swagger-ui.html`.

## 2. Conventions

| Topic | Rule |
|---|---|
| Versioning | Major version in the path (`/api/v1`). Additive changes (new fields, endpoints) stay in v1; breaking changes go to `/api/v2` |
| Date-times | ISO-8601 local date-time without offset, `2026-10-12T09:30:00`, always Vietnam time (ADR-003) |
| Dates | `yyyy-MM-dd` |
| IDs | UUID strings |
| JSON | camelCase fields; enums as upper-case strings; `null` fields are included (e.g. `cancelledAt: null`) |
| Cancel as an action | `POST /appointments/{id}/cancel` rather than `PATCH`, because it is a state transition with side effects, not a field edit. It is idempotent by design |
| Request tracing | Optional `X-Request-Id` request header; always present in the response |
| Idempotency | Required `Idempotency-Key` header on `POST /appointments` (ADR-004) |

## 3. Error model

All errors use RFC 7807 `application/problem+json`, built with Spring's `ProblemDetail`:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "No technician is available for the selected time. Please choose another time.",
  "instance": "/api/v1/appointments",
  "code": "NO_QUALIFIED_TECHNICIAN_AVAILABLE",
  "requestId": "6f1c2a8e-4b1d-4c55-9a43-0a6f0c2f7e11"
}
```

- `code` is the stable, machine-readable value from the catalogue in `00-conventions.md`. Clients branch on `code`, never on `detail`.
- `detail` is a human-readable message the UI can show as is (GR-11). Messages are defined once, in `ErrorCode`.
- Validation errors add `errors: [{ "field": "startTime", "message": "must be a local date-time without offset, e.g. 2026-10-12T09:30:00" }]`.
- Unexpected errors return 500 with a generic `detail` and the `requestId`; the stack trace is logged, never returned.

| Status | Meaning | Client reaction |
|---|---|---|
| 400 | The request is malformed | Fix the request (a programming error in the client) |
| 404 | A referenced resource does not exist | Refresh reference data |
| 409 | Valid, but conflicts with the current schedule | Show `detail`, refresh availability, let the customer pick again |
| 422 | Well-formed, but breaks a business rule | Show `detail`, let the customer change the input |
| 500 | Server fault | Retry later with the same `Idempotency-Key` |

## 4. Contract delivery

- **Code-first OpenAPI** with springdoc-openapi: the spec is generated from controllers and DTOs, so it cannot drift from the code. Controllers carry `@Operation` and `@ApiResponse` annotations for each documented error status (AC-E03.1).
- The generated spec is exported to `docs/openapi.yaml` and committed. A test compares the committed file with the generated one, so a contract change is always a visible diff in review (AC-E03.3).
- **Client harness:** Swagger UI for interactive use, `docs/api-requests.http` for IntelliJ or VS Code REST clients, and `scripts/demo.sh` (curl) for the scripted demo (AC-E03.4).
