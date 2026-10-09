# T18: API documentation and demo harness

**Story:** E-03 (AC-E03.1 to AC-E03.4) · **Type:** single task

## Read first

- `docs/user-stories/E-03-api-documentation.md`
- `docs/system-design/06-api.md`
- The controller classes (one at a time, when annotating them)

## Before starting (EM)

Look up the springdoc-openapi version that supports Spring Boot 4.1 (springdoc's README or GitHub releases) and give it to the agent. The agent must not guess a version.

## Files

| Action | File |
|---|---|
| Modify | `gradle/libs.versions.toml`, `build.gradle.kts` (add `springdoc-openapi-starter-webmvc-ui`) |
| Modify | all controllers (`@Tag`, `@Operation`, `@ApiResponse` for each documented error status) |
| Create | `src/test/java/com/keyloop/scheduler/OpenApiContractTest.java` |
| Create | `docs/openapi.yaml` (generated, see below) |
| Create | `docs/api-requests.http` |
| Create | `scripts/demo.sh` |

## Specification

- **OpenAPI contract test** (extends `IntegrationTest`): fetch `/v3/api-docs.yaml`. If the system property `updateOpenApi=true` is set, write it to `docs/openapi.yaml`; otherwise assert it equals the committed file. Generate with `.\gradlew.bat test --tests "*OpenApiContractTest" -DupdateOpenApi=true` (forward the property in `tasks.test` with `systemProperty`).
- **`api-requests.http`:** the AC-E03.4 sequence with fixture IDs, runnable from IntelliJ or the VS Code REST Client.
- **`scripts/demo.sh`** (bash + curl + `jq` if available): the same sequence against `http://localhost:8080`, printing each step's HTTP status and key fields. The conflicting booking uses EV_BATTERY_CHECK at a slot already taken by the first booking with a different customer.

## Done when

- `OpenApiContractTest` passes; `/swagger-ui.html` loads (EM checks in a browser).
- EM runs `scripts/demo.sh` against `docker compose up` and sees 201, 200, 409, 200, 201 in order.
