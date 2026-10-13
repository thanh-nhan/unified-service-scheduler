# Unified Service Scheduler

A backend service that lets a customer book a vehicle service appointment at a dealership. The system checks opening hours, finds a technician with the right skill and a bay of the right type, and **guarantees that no technician, bay or vehicle is ever double-booked**, even when many customers book the same slot at the same moment.

This is my submission for **Scenario A (Unified Service Scheduler)**, backend layer only: Java 21, Spring Boot 4.1, PostgreSQL 17.

**Video walkthrough:** _add link here_

## Contents

1. [What it does](#what-it-does)
2. [Quick start](#quick-start)
3. [Prerequisites](#prerequisites)
4. [Build](#build)
5. [Run](#run)
6. [Test](#test)
7. [Try the API](#try-the-api)
8. [Repository map](#repository-map)
9. [Design in brief](#design-in-brief)
10. [AI Collaboration Narrative](#ai-collaboration-narrative)
11. [Known limitations](#known-limitations)

## What it does

| Capability | Endpoint | Story |
|---|---|---|
| Browse dealerships and service types | `GET /api/v1/dealerships`, `GET /api/v1/service-types` | US-01 |
| Daily availability for a service | `GET /api/v1/dealerships/{id}/availability?serviceTypeId=&date=` | US-02 |
| Weekly availability (Monday to Sunday) | `GET /api/v1/dealerships/{id}/availability/weekly?serviceTypeId=&weekStart=` | US-09 |
| Book (system assigns technician and bay) | `POST /api/v1/appointments` with an `Idempotency-Key` header | US-03, US-04, US-05 |
| View an appointment | `GET /api/v1/appointments/{id}` | US-06 |
| Cancel an appointment | `POST /api/v1/appointments/{id}/cancel` | US-07 |
| List a customer's appointments | `GET /api/v1/customers/{id}/appointments` | US-08 |
| Health, metrics, API docs | `/actuator/health`, `/actuator/prometheus`, `/swagger-ui.html` | E-02, E-03 |

Key rules: one service per appointment; a single time zone (Vietnam local time, no offsets); a booking that cannot be served returns a precise `409` with a stable error `code` (no alternative slots, see [ADR-006](docs/system-design/adr/ADR-006-no-alternative-suggestions.md)); retrying a booking with the same `Idempotency-Key` returns the original appointment.

## Quick start

Needs **Docker** and nothing else (the image builds the app itself):

```bash
docker compose up --build
```

Wait until `http://localhost:8080/actuator/health` reports `"status":"UP"`, then open `http://localhost:8080/swagger-ui.html`, or run `bash scripts/demo.sh` (see [Try the API](#try-the-api)).

Stop and delete the data with `docker compose down -v`.

## Prerequisites

| Tool | Version | Needed for |
|---|---|---|
| JDK | 21 | Building and testing locally (Gradle selects a Java 21 toolchain) |
| Docker (Desktop on Windows/macOS) | any recent | Database for running the app, and **Testcontainers for the tests** |
| Bash and `curl` | any | `scripts/demo.sh` only (Git Bash on Windows). `jq` is optional |

Gradle itself is not needed: use the wrapper (`gradlew` / `gradlew.bat`). All examples use the Windows form; on macOS and Linux use `./gradlew`.

## Build

```powershell
.\gradlew.bat build --rerun-tasks
```

This compiles the code, **runs all tests** and produces `build/libs/*.jar`. **Docker must be running**, because the tests start a real PostgreSQL container. If Docker is not reachable, every database test fails with `Previous attempts to find a Docker environment failed`; start Docker and run again.

To build the jar without tests: `.\gradlew.bat bootJar`.

## Run

### Option A: everything in Docker (simplest)

```bash
docker compose up --build
```

Starts PostgreSQL 17 and the application (profile `dev`, which also loads the demo fixture data). The app listens on port 8080 and the database on 5432.

### Option B: app on your machine, database in Docker (for development)

```powershell
docker compose up -d db
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

The `dev` profile adds the fixture data (2 dealerships, 3 service types, technicians, bays, customers and vehicles) and JSON console logs. Without it you get an empty catalogue.

### Configuration

| Variable | Default | Meaning |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/scheduler` | JDBC URL |
| `DB_USER` / `DB_PASSWORD` | `scheduler` / `scheduler` | Local development credentials only |
| `OTEL_TRACING_EXPORT_ENABLED` | `false` | Export traces over OTLP (set the standard `OTEL_EXPORTER_OTLP_*` variables too) |
| `OTEL_METRICS_EXPORT_ENABLED` | `false` | Push metrics over OTLP. Prometheus scraping at `/actuator/prometheus` is the default path |

The JVM runs with `-Duser.timezone=Asia/Ho_Chi_Minh` (set in `build.gradle.kts` and the Dockerfile). PostgreSQL rejects the legacy alias `Asia/Saigon`, so use the canonical name if you start the JVM yourself.

### Reset the data

The demo only creates appointments, so this returns you to the seed state:

```bash
docker compose exec -T db psql -U scheduler -d scheduler -c "TRUNCATE appointment;"
```

For a completely fresh database (schema and fixture re-applied): `docker compose down -v && docker compose up --build`.

## Test

Tests use a **real PostgreSQL 17 in a Testcontainer** (never H2, never mocked repositories), because the correctness guarantee lives in database constraints. Docker must be running.

```powershell
# everything
.\gradlew.bat test --console=plain

# one class
.\gradlew.bat test --tests "com.keyloop.scheduler.acceptance.ReliableBookingAcceptanceTest" --console=plain

# the architecture rules only
.\gradlew.bat test --tests "com.keyloop.scheduler.ArchitectureTest" --console=plain
```

HTML report: `build/reports/tests/test/index.html`.

| Kind | Where | What it proves |
|---|---|---|
| Acceptance tests | `src/test/java/.../acceptance/` | One test per acceptance criterion in the user stories. The `@DisplayName` starts with the AC ID, e.g. `AC-05.1`. These files carry a `// FROZEN` header: they were reviewed before the implementation and not changed to make code pass |
| Concurrency | `ReliableBookingAcceptanceTest` | AC-05.1: 20 parallel requests for the same slot give exactly one `201`, nineteen `409`, no `5xx` and one row in the database |
| Repository and SQL | `booking/`, `catalog/`, `SchemaTest` | Allocation queries, exclusion constraints, schema validation |
| Pure unit tests | `availability/`, `common/` | Slot calculation, scheduling rules, strict date-time parsing, error mapping |
| Architecture | `ArchitectureTest` | Module dependencies, controllers do not touch repositories, no `LocalDateTime.now()` without a clock |
| API contract | `OpenApiContractTest` | `docs/openapi.yaml` matches the running application |

Which acceptance criteria have tests:

```bash
grep -rhoE '"AC-[A-Z0-9]+\.[0-9]+' src/test/java | sort -u
```

If you change the API on purpose, regenerate the contract: `.\gradlew.bat test --tests "*OpenApiContractTest" -DupdateOpenApi=true`.

## Try the API

### Swagger UI

`http://localhost:8080/swagger-ui.html` (the spec is also committed as [docs/openapi.yaml](docs/openapi.yaml)).

### Scripted demo

```bash
bash scripts/demo.sh
```

Runs seven steps against the fixture data and prints each status code: list service types, check availability, **book (201)**, **replay the same `Idempotency-Key` (200)**, **a second customer takes the same slot (409)**, **cancel (200)**, **rebook the freed slot (201)**. Reset the data first if you ran it before (see [Reset the data](#reset-the-data)); a second run on dirty data fails because the slot is taken.

The same sequence is in [docs/api-requests.http](docs/api-requests.http) for IntelliJ and VS Code REST Client.

### One request by hand

```bash
curl -i -X POST http://localhost:8080/api/v1/appointments \
  -H "Idempotency-Key: my-first-booking" -H "Content-Type: application/json" \
  -d '{"customerId":"50000000-0000-0000-0000-000000000001","vehicleId":"60000000-0000-0000-0000-000000000001","dealershipId":"10000000-0000-0000-0000-000000000001","serviceTypeId":"40000000-0000-0000-0000-000000000003","startTime":"2030-01-15T09:00:00"}'
```

Errors are RFC 7807 `application/problem+json` with a stable `code` and a `requestId` that also appears in the `X-Request-Id` response header and in every log line for that request.

### Observability

| What | Where |
|---|---|
| Health, liveness, readiness | `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` |
| Metrics (Prometheus) | `/actuator/prometheus`: `scheduler_booking_attempts_total`, `scheduler_booking_duration_seconds`, `scheduler_booking_conflict_retries_total`, `scheduler_availability_duration_seconds` |
| Logs | JSON, one `booking.completed` event per booking request; IDs only, no names, emails, phones or VINs |

## Repository map

```text
.
├── README.md                  this file
├── CONVENTIONS.md             coding rules given to the AI coding agent
├── build.gradle.kts           Gradle (Kotlin DSL); versions in gradle/libs.versions.toml
├── compose.yaml, Dockerfile   PostgreSQL 17 + the application
├── scripts/demo.sh            seven-step API demo
├── src/main/java/com/keyloop/scheduler/
│   ├── catalog/               dealerships, opening hours, service types, technicians, bays
│   ├── customer/              customers and vehicles
│   ├── availability/          pure slot calculator + daily/weekly endpoints
│   ├── booking/               booking, idempotency, cancellation, queries
│   └── common/                errors, request ID, date-time parsing, constraint helpers
├── src/main/resources/db/     Flyway migrations (schema) and fixture data
├── src/test/java/...          unit, repository, acceptance and architecture tests
└── docs/
    ├── user-stories/          WHAT: stories and numbered acceptance criteria
    ├── system-design/         HOW and WHY: design documents and ADRs
    ├── tasks/                 the task breakdown given to the coding agent
    ├── ai-collaboration-log.md  running log of AI mistakes and corrections
    ├── openapi.yaml           generated API contract
    └── api-requests.http      the demo as HTTP requests
```

## Design in brief

The full System Design Document is in [docs/system-design/](docs/system-design/README.md): architecture diagram, data flow, data model, concurrency, availability, API, technology choices, observability, scalability, and how GenAI was used. The requirements are in [docs/user-stories/](docs/user-stories/README.md).

| Decision | Choice | Why |
|---|---|---|
| Double-booking | PostgreSQL **exclusion constraints** on technician, bay and vehicle | The database is the referee. A "check then insert" race cannot slip through. Compared against row locks, advisory locks and SERIALIZABLE ([ADR-002](docs/system-design/adr/ADR-002-database-enforced-overlap.md)) |
| Lost races | Up to 3 retries, each in a **new transaction** in a separate bean; deadlocks (`40P01`) count as lost races | After a constraint violation PostgreSQL aborts the transaction; the final answer is `409 HIGH_CONTENTION` if all attempts lose ([04](docs/system-design/04-booking-and-concurrency.md)) |
| Retried requests | `Idempotency-Key` stored on the appointment with a request hash | A duplicate returns the original result; a different body with the same key returns `422` ([ADR-004](docs/system-design/adr/ADR-004-idempotency.md)) |
| Architecture | Modular monolith, one database | One team, one transaction boundary for booking ([ADR-001](docs/system-design/adr/ADR-001-modular-monolith.md)) |
| Time | One time zone (Vietnam), `LocalDateTime`, injected `Clock`, strict parsing that rejects `Z` and offsets | The business has no cross-border customers ([ADR-003](docs/system-design/adr/ADR-003-single-time-zone.md)) |
| Platform | Java 21, Spring Boot 4.1, Gradle, Flyway, Testcontainers | Spring Boot 3.5 open-source support ended on 2026-06-30 ([ADR-005](docs/system-design/adr/ADR-005-spring-boot-4.md)) |
| Conflicts | `409` with a code only, no alternative slots | The UI picks from the availability view ([ADR-006](docs/system-design/adr/ADR-006-no-alternative-suggestions.md)) |

Availability is a snapshot, not a reservation: only the booking endpoint decides.

---

## AI Collaboration Narrative

Details and the full list of incidents are in [docs/ai-collaboration-log.md](docs/ai-collaboration-log.md). How AI was used in the design phase is in [system-design/10-genai-in-design.md](docs/system-design/10-genai-in-design.md).

### 1. Strategy: documents are the contract

I approached this as an engineering manager working with a product owner role (also mine) and two kinds of AI:

| Role | Who | Responsibility |
|---|---|---|
| Product owner and engineering manager | Me | Owns scope; approves every document; reviews every plan and diff; makes all commits |
| Planner | Claude (chat) | Compares options and trade-offs, drafts stories, design, ADRs and the task breakdown |
| Coder | Aider with qwen2.5-coder:14b (local) for T01-T03; Claude Code from T03 session 2 onward | Implements one small task at a time |

My version of specification-driven development is a chain of approvals. Nothing moves to the next link until I approve the previous one:

```text
user stories (v0.1 → v0.4)  →  system design (v0.1 → v0.3)  →  task breakdown (T00-T19)
        →  acceptance tests written first and frozen  →  implementation  →  review and commit
```

What made this work with a weak model, and later a strong one:

- **Stories with numbered acceptance criteria and exact values** (slot counts, error codes, technician and bay IDs). Every AC became one test, so there was nothing for the coder to interpret.
- **Tests first, in a separate session from the code.** Part A of each story task writes the acceptance tests from the AC. I review them line by line against the story, then add `// FROZEN`. Part B is a fresh session that implements until the frozen tests pass, without editing them. The agent that writes the code never decides what "correct" means.
- **A conventions file** ([CONVENTIONS.md](CONVENTIONS.md)) loaded into every session: Spring Boot 4 package differences, injected `Clock` only, `JdbcClient` with named parameters, retry loop outside the transaction, no Lombok. Docs, conventions and frozen tests were read-only for the coder.
- **Small tasks.** Each task lists the files it may touch, the documents to read first, a "watch for" list and a single test command. When a task was too large for the 16k-token local model I split it.
- **Plan before code.** The coder first restates the goal, the AC IDs and the change per file; I correct the plan before it writes anything.
- **Git is mine.** Agents never ran `git`; every task part is one commit that I made after review.

### 2. Verification and refinement

| Technique | Example |
|---|---|
| Check facts that change over time | The planner recommended Spring Boot 3.x and a model name that does not exist ("Qwen3.5 Coder"). I checked support dates and model names; Boot 3.5 had lost open-source support on 2026-06-30, so the project is on 4.1 |
| Recompute every number in the stories | Slot counts (35, 31, 21, 26) were calculated by hand before they became test assertions |
| Cross-document consistency | After each product change, all story files were searched for leftover references |
| Verify against the filesystem, not the chat | The first harness (Cline) reported files created that did not exist; I found it by looking at the disk |
| Review every agent plan and diff | Review checklists per task, e.g. "`BookingService` is not `@Transactional`, `saveAndFlush` is used, the retry loop is in the service, the response has no email or phone" |
| Repeat concurrency tests | The concurrency class was run three times in a row, and a full `--rerun-tasks` build before trusting a green result |
| Spec-vs-implementation review | After T19, I had the planner compare the whole coding transcript with the stories, design and conventions. It found two places where the **design** was behind the code (below) and listed process deviations |
| Architecture tests | ArchUnit rules enforce module boundaries and "no `now()` without a clock" so reviewers do not have to |

### 3. What went wrong, and what I did about it

- **Planner errors I caught.** It proposed multi-time-zone support and alternative-slot suggestions, which the business did not need; I removed both (ADR-003, ADR-006). I added the weekly availability story (US-09) myself. It also assumed a non-existent model name and an unsupported framework line.
- **The local coder was too weak.** I started with Cline and a 30B model that did not fit my laptop, then qwen2.5-coder:14b. Cline reported edits that never happened, so I moved to Aider. Aider did not fail loudly; it produced plausible but wrong code in T01-T03:
  - **Build files:** version-catalog (TOML) and Kotlin DSL syntax were mixed up (`version.ref` with the wrong prefix, a dashed catalog key used as a Kotlin accessor, `apply false`), so the build failed.
  - **Imports:** missing Java imports, and the wrong `TestcontainersConfiguration` class.
  - **Wrong API choices:** `JdbcTemplate` where the task said `JdbcClient`; an undeclared `dataSource`; a database column that does not exist; a clock test helper that never controlled the clock.
  - Some of this was **my** fault: the T01 and T02 task files did not say enough (foreign keys, `@Import`). I corrected the task files.
  - From T03 session 2 I switched to Claude Code. The process did not change; the stories, design and task files needed no rewrite.
- **A test found what design review missed.** The concurrency test failed about one run in ten. Two requests that conflict on technician and bay in opposite order cause a PostgreSQL deadlock (`40P01`), which Spring reports as a different exception, so the retry loop turned it into a `500`. A related test showed that a duplicate request could receive a `409` instead of the winner's `200`. I wrote both fixes back into the design (v0.3, rule 5 and rule 7 in [04](docs/system-design/04-booking-and-concurrency.md)).
- **Frozen-test rule bent once.** In T17, fixing the implementation also needed three fixes in the (not yet frozen) test; the agent disclosed this and I re-reviewed the file.
- **Human input is fallible too.** The agent refused to guess library versions and asked me; I gave it a wrong ArchUnit version, and it used a real one.

### 4. How I ensured the final quality

1. **Behaviour is pinned by tests that were written before the code**, one per acceptance criterion, against a real PostgreSQL.
2. **The key invariant is enforced by the database**, not by application logic that could contain the same bug as the code under test.
3. **Every document and plan had a human approval gate**, recorded in change logs (stories v0.4, design v0.3).
4. **Mechanical guards:** frozen tests, a read-only docs directory, ArchUnit rules, an OpenAPI contract test, and `--rerun-tasks` builds.
5. **Honest records:** the [AI collaboration log](docs/ai-collaboration-log.md) lists the mistakes and how each was caught.

## Known limitations

- Tracing: spans for booking attempt and allocation, and the `appointment.cancelled` log event, are in the [observability design](docs/system-design/08-observability.md) but not implemented. Trace and span IDs in log lines were not verified.
- Availability is a point-in-time view; a slot shown as free can be taken before the customer books.
- Single dealership time zone and no authentication (customers are identified by ID), by scope.
- The 14B local model was used only for T01-T03; later tasks used Claude Code, so "local model only" is not claimed.
