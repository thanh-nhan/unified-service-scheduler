# Coding conventions: Unified Service Scheduler

You are the coding agent, working through Aider. A human Engineering Manager (EM) directs you and approves every change. This file is always loaded read-only.

## 1. Sources of truth (read-only for you)

- `docs/user-stories/` defines WHAT to build. Acceptance criteria (AC) are exact; do not reinterpret them.
- `docs/system-design/` defines HOW to build it.
- `docs/tasks/Txx-*.md` defines the current task: scope, files, and done criteria.
- If a task, story, and design disagree, or something is unclear: STOP and ask the EM. Never guess or invent requirements.

## 2. Working rules

1. Do exactly the task part the EM names, nothing more.
2. When asked to plan (`/ask`), restate the goal, the AC IDs covered, and the change you will make to each file. Do not write code in that answer.
3. Edit only files the EM has added to the chat. If you need another file, say which one and why, and stop.
4. Files loaded read-only (`docs/`, `CONVENTIONS.md`, frozen tests) must never be changed. Test files containing `// FROZEN` are the contract: if one seems wrong, explain why and stop.
5. NEVER weaken a test to make it pass: no `@Disabled`, no deleting assertions, no loosening expected values.
6. Do not add dependencies unless the task says so. Versions go in `gradle/libs.versions.toml` only.
7. Always output complete files. Never use placeholders such as `// ... rest unchanged`.
8. When the EM runs a test and it fails, fix the cause in the code under test, not the test.

## 3. Commands (Windows)

- The EM runs tests with Aider's `/test` command, e.g. `/test .\gradlew.bat test --tests "com.keyloop.scheduler.availability.AvailabilityCalculatorTest" --console=plain`
- Full build: `.\gradlew.bat build --console=plain`
- Docker Desktop must be running (Testcontainers).

## 4. Stack (Spring Boot 4, NOT Spring Boot 3)

Java 21, Spring Boot 4.1, Gradle Kotlin DSL (`build.gradle.kts`), PostgreSQL 17, Flyway, Testcontainers, JUnit 5, AssertJ.

Spring Boot 4 differences you MUST follow:

| Do NOT write (Boot 3 habit) | Write instead (Boot 4) |
|---|---|
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| Flyway with only `flyway-core` | `spring-boot-starter-flyway` + `flyway-database-postgresql` |
| `spring-boot-starter-test` alone | test starters such as `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test` |
| `com.fasterxml.jackson.databind.*` | `tools.jackson.databind.*` (annotations stay `com.fasterxml.jackson.annotation.*`) |
| `@MockBean`, `@SpyBean` | `@MockitoBean`, `@MockitoSpyBean` |
| MockMvc from `@SpringBootTest` alone | add `@AutoConfigureMockMvc` |
| `org.testcontainers:postgresql` | `org.testcontainers:testcontainers-postgresql` |
| Maven `pom.xml` snippets | Gradle Kotlin DSL: `implementation(libs.xxx)`, `testImplementation(...)` |

If a class or package does not resolve, say so and check the actual dependency rather than guessing another name.

## 5. Code conventions

- Package root `com.keyloop.scheduler`; modules `common`, `catalog`, `customer`, `availability`, `booking` exactly as in `docs/system-design/02-architecture.md` §4.
- Constructor injection only; fields `private final`. No Lombok. No field injection.
- DTOs are Java `record`s in a `dto` subpackage. Controllers return DTOs, never entities.
- Controllers call services only. Services call repositories.
- Errors: throw `ApiException(ErrorCode.X)`. Never build error responses in controllers. Never catch `Exception` broadly.
- Time: only `LocalDateTime.now(clock)` / `LocalDate.now(clock)` with the injected `Clock`. NEVER `LocalDateTime.now()` without a clock. NEVER `now()` or `CURRENT_TIMESTAMP` in SQL.
- Date-times are `LocalDateTime` (Vietnam local time, no offset). No `ZonedDateTime`, `OffsetDateTime`, or `Instant` in the domain or API.
- Enums stored as strings (`@Enumerated(EnumType.STRING)`).
- Logging: SLF4J. No `System.out`. Never log names, emails, phone numbers, or VINs; IDs only.
- Keep classes small; no speculative abstractions, interfaces with one implementation, or unused code.

## 6. Persistence rules

- Schema changes only via new Flyway files in `src/main/resources/db/migration`. Never edit an existing migration.
- Hibernate `ddl-auto=validate`. Never `create` or `update`.
- Never map `appointment.time_range` in JPA (it is a generated column).
- Native SQL through `JdbcClient` with named parameters. Never concatenate values into SQL.
- "Lowest id" ordering is done in SQL (`ORDER BY id`). Never sort UUIDs in Java.

## 7. Booking rules (critical, see design 04 §4)

- `BookingService` is NOT `@Transactional`. Each attempt runs in `BookingTransaction.tryAllocateAndInsert`, a SEPARATE bean annotated `@Transactional`.
- Never catch the constraint exception inside the transactional method.
- Insert with `saveAndFlush`.
- Identify constraint violations by SQLSTATE (`23P01`, `23505`) and constraint name via `ConstraintViolations`, never by message text.
- After any constraint violation, check the idempotency key FIRST, then the constraint name. Maximum 3 attempts.
- "Check, then insert" without the database constraints is NOT acceptable.

## 8. Testing rules

- Integration tests extend `IntegrationTest` (real PostgreSQL via Testcontainers, fixture reloaded before each test). NEVER use H2. NEVER mock repositories in integration tests.
- Use the constants in `Fixture` (D1, T1, B1, C1, V1, S1, ...) instead of literal UUIDs.
- Seed appointments with `AppointmentSeeder`, not through the API, when an AC says "T1 has an appointment ...".
- Acceptance tests: one test (or `@ParameterizedTest`) per AC, `@DisplayName` starting with the AC ID, e.g. `@DisplayName("AC-03.2: busy technician is avoided")`.
- Assert exact values from the AC (status, `code`, times, technician and bay IDs, counts).
- Concurrency tests use `ExecutorService` + `CountDownLatch` so requests really run in parallel.
