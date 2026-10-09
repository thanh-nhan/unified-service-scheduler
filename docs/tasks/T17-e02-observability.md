# T17: Observability

**Story:** E-02 (AC-E02.3 to AC-E02.6; AC-E02.1 and AC-E02.2 were done in T05) · **Parts:** A, B

## Read first (both parts)

- `docs/user-stories/E-02-observability.md`
- `docs/system-design/08-observability.md` §2, §3, §4

---

## Part A: acceptance tests

**Create:** `src/test/java/com/keyloop/scheduler/acceptance/ObservabilityAcceptanceTest.java`.

- AC-E02.4: capture logs with an in-memory Logback `ListAppender` attached to the `BookingService` logger. Make one successful and one rejected booking; assert exactly one `booking.completed` event each, with the required key-value pairs, and that no event contains the fixture customer name, email, or VIN.
- AC-E02.5: make one successful booking and one `NO_QUALIFIED_TECHNICIAN_AVAILABLE` booking, then `GET /actuator/prometheus` and assert the listed lines and metric names are present. The test's Spring context must use a fresh `MeterRegistry` or compare counts before and after, because other tests share the context.
- AC-E02.6: `/actuator/health/liveness` and `/actuator/health/readiness` return `UP`, and readiness includes `db`.
- AC-E02.3 (JSON logs in `dev`) is verified by the EM by hand: `docker compose up` and inspect a log line. No automated test.

**Then (EM):** review, freeze, commit.

---

## Part B: implementation

**Also read:** `booking/BookingService.java`, `src/main/resources/application.yml`, `src/main/resources/application-dev.yml`.

**Files:**

| Action | File |
|---|---|
| Create | `booking/BookingMetrics.java` |
| Modify | `booking/BookingService.java` (metrics, timer, one `booking.completed` event in a `finally` block, `booking.conflict.retry` event) |
| Modify | `availability/AvailabilityService.java` (timer `scheduler.availability.duration`, tag `range`) |
| Modify | `application.yml`, `application-dev.yml` |
| Modify | `build.gradle.kts`, `gradle/libs.versions.toml` (only if the OpenTelemetry starter is missing; ask the EM for the coordinates) |

**Specification:**
- Metric names, types, and tags exactly as in 08 §3. Timer with `publishPercentileHistogram()`.
- Log events use `log.atInfo().addKeyValue(...)`.
- `application-dev.yml`: `logging.structured.format.console: logstash`.
- `application.yml`: health probes enabled, readiness group includes `db`; tracing sampling probability 1.0; trace export off unless an environment variable enables it.

**Watch for (EM):** no IDs as metric tags; exactly one `booking.completed` per request even when an exception escapes.

**Done when:** the frozen class passes unchanged; run `.\gradlew.bat build` for the whole suite.
