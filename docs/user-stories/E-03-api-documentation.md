# E-03: API documentation and client harness (enabler)

**Priority:** Must · **Depends on:** US-07 · **Status:** Draft

## Story

**As an** API consumer or reviewer
**I want** an accurate API contract and a ready-made way to call the API
**So that** I can understand and try the service without reading the code

## Acceptance criteria

**AC-E03.1: Generated OpenAPI document**
When the client calls `GET /v3/api-docs`
Then it returns an OpenAPI 3 document listing every `/api/v1` endpoint, with request and response schemas
And each endpoint documents the error statuses it can return (400, 404, 409, 422, as applicable)

**AC-E03.2: Swagger UI**
Then `GET /swagger-ui.html` serves an interactive UI for the API

**AC-E03.3: Committed contract stays in sync**
Given `docs/openapi.yaml` is committed
When `./mvnw verify` runs
Then a test fails if the committed file differs from the generated document

**AC-E03.4: Demo script**
Given the application is running via Docker Compose with `dev` seed data
When `scripts/demo.sh` (curl) or `docs/api-requests.http` runs
Then it performs, in order: list service types → availability → book (201) → replay with the same key (200) → conflicting booking (409 with a readable `detail`) → cancel (200) → rebook the freed slot (201)
And every step prints its HTTP status and the key response fields
