# 07: Technology choices

Versions are pinned in the Gradle version catalog (`gradle/libs.versions.toml`), with the Spring Boot Gradle plugin managing Spring-related versions and the Gradle Wrapper fixing the Gradle version, so a clean clone builds identically. "Latest patch" means the newest patch release available when implementation starts.

| Area | Choice | Why | Alternatives considered |
|---|---|---|---|
| Language | **Java 21 (LTS)** | Long-term support, records, pattern matching, virtual threads. Widely known by the team and by the coding model | Java 25 (newer LTS, less tooling and model familiarity); Kotlin |
| Framework | **Spring Boot 4.1.x** (latest patch), Spring MVC | Mature REST, validation, data access, and observability in one stack. 3.5 is out of open-source support (ADR-005) | Spring Boot 3.5 (EOL), Quarkus, Micronaut |
| Concurrency model | Servlet stack with **virtual threads** enabled (`spring.threads.virtual.enabled=true`) | Simple blocking code; cheap threads for I/O waits | WebFlux (reactive complexity with no benefit at this scale) |
| Database | **PostgreSQL 17** | Exclusion constraints and range types guarantee no double-booking at the database level (ADR-002). Mature, managed offerings everywhere | MySQL (no exclusion constraints; see ADR-002), MongoDB (no equivalent constraint) |
| Data access | **Spring Data JPA (Hibernate)** for CRUD; **`JdbcClient`** with native SQL for allocation and availability queries | JPA for simple entity work; plain SQL where range operators (`&&`, `tsrange`) and exact query plans matter | jOOQ (extra tooling), JPA-only (awkward with range types) |
| Migrations | **Flyway** (`spring-boot-starter-flyway`) | Versioned, reviewable SQL; constraints live in SQL where they are visible | Liquibase (XML/YAML adds indirection) |
| Validation | Jakarta Bean Validation | Declarative request validation with field-level errors | Manual checks |
| API docs | **springdoc-openapi** (Spring Boot 4 compatible line) | Generates OpenAPI 3 and Swagger UI from code | Hand-written spec (drifts from code) |
| Testing | **JUnit 5, AssertJ, Testcontainers 2** (`testcontainers-postgresql`), Spring Boot test starters | Integration tests run against real PostgreSQL, so exclusion constraints and SQL are actually tested | H2 (does not support exclusion constraints; tests would pass while production fails) |
| Metrics | **Micrometer** with the Prometheus registry | Standard in Spring Boot; Prometheus is the de facto open-source metrics store | Vendor agents |
| Logging | Spring Boot **structured logging** (JSON, Logstash format) over SLF4J/Logback | Built in, no extra library; includes MDC fields such as `requestId` | Custom Logback JSON encoder |
| Tracing | **OpenTelemetry** via `spring-boot-starter-opentelemetry` (Micrometer Tracing bridge) | Vendor-neutral traces; trace and span IDs in every log line | Zipkin/Brave (narrower ecosystem) |
| Build | **Gradle** with Gradle Wrapper, **Kotlin DSL** (`build.gradle.kts`) and a version catalog | Reviewers need only a JDK. Fast incremental builds and test filtering (`./gradlew test --tests ...`); the Kotlin DSL is Gradle's default and gives type-checked build scripts with clear errors | Maven (more verbose XML, slower incremental builds); Gradle Groovy DSL (less IDE checking) |
| Runtime packaging | Docker image (multi-stage build, JRE 21 base) and **Docker Compose** | One command to run the API and database (AC-E01.2) | Kubernetes manifests (out of scope; described in 09) |
| AI-assisted development | **Cline** in VS Code with **qwen3-coder:30b** via **Ollama**, 32k context | Fully local coding model. Directed by approved stories and this design. See 10 and the README's AI Collaboration Narrative | Aider (kept as fallback) |

## Spring Boot 4 notes for the coding model

The coding model has seen far more Spring Boot 3 code than Spring Boot 4 code. These differences go into `.clinerules` so it does not generate Spring Boot 3 code:

| Spring Boot 3 habit | Spring Boot 4 |
|---|---|
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| Flyway via `flyway-core` only | `spring-boot-starter-flyway` (plus `flyway-database-postgresql`) |
| `spring-boot-starter-test` | Per-technology test starters, e.g. `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test` |
| `com.fasterxml.jackson.databind.*` | Jackson 3: `tools.jackson.*` (annotations stay in `com.fasterxml.jackson.annotation`) |
| `@MockBean` | `@MockitoBean` |
| `@SpringBootTest` gives `MockMvc` | Add `@AutoConfigureMockMvc` explicitly |
| `org.testcontainers:postgresql` | `org.testcontainers:testcontainers-postgresql` |
| Maven `pom.xml` examples | Gradle Kotlin DSL: `implementation("...")`, `testImplementation("...")`, `testRuntimeOnly("org.junit.platform:junit-platform-launcher")`, versions from `libs.versions.toml` |

The implementation spike (task 0) confirms these, and the Gradle version required by Spring Boot 4.1, against the actual dependency versions before feature work starts.
