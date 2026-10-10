# T00: Generate the project skeleton (EM, no agent)

**Story:** E-01 · **Done by:** EM by hand · **Time:** about 20 minutes

Spring Initializr produces the correct Spring Boot 4.1 dependency coordinates, Gradle Wrapper, and Testcontainers setup. Starting from it avoids the coding model guessing Spring Boot 3 artifact names.

## Steps

1. Open https://start.spring.io and choose:

   | Field | Value |
   |---|---|
   | Project | Gradle - Kotlin |
   | Language | Java |
   | Spring Boot | latest **4.1.x** release (not SNAPSHOT or M/RC) |
   | Group | `com.keyloop` |
   | Artifact / Name | `scheduler` |
   | Package name | `com.keyloop.scheduler` |
   | Packaging | Jar |
   | Java | 21 |

2. Add dependencies (names as shown in Initializr): **Spring Web**, **Validation**, **Spring Data JPA**, **PostgreSQL Driver**, **Flyway Migration**, **Spring Boot Actuator**, **Prometheus**, **Testcontainers**. If **OpenTelemetry** is listed, add it too; otherwise T17 adds it.
3. Click **Explore** and note the versions shown (Spring Boot, Gradle in `gradle-wrapper.properties`, Testcontainers). Record them in `docs/ai-collaboration-log.md` under Process changes.
4. Download and unzip into the repo root, keeping the existing `.git` and `docs/`. The main class should be `src/main/java/com/keyloop/scheduler/SchedulerApplication.java`. Rename it if Initializr generated a different name.
5. Start Docker Desktop, then run:

   ```powershell
   .\gradlew.bat build --console=plain
   ```

   The generated `contextLoads` test starts a PostgreSQL container. A green build proves Java, Gradle, Docker, and Testcontainers all work on this machine.
6. Commit: `build: generate Spring Boot 4.1 skeleton from Spring Initializr`.

## If the build fails

| Symptom | Likely cause |
|---|---|
| `Could not find a valid Docker environment` | Docker Desktop not running |
| Flyway: `No migrations found` warning | Expected until T02 |
| Unsupported class file / toolchain error | JDK 21 not installed or not selected; install Temurin 21 |

## Done when

- `.\gradlew.bat build` is green on a clean checkout.
- The versions are recorded in the log.
