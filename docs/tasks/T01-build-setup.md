# T01: Build setup and Spring Boot 4 check

**Story:** E-01 (AC-E01.1) · **Type:** single task, two steps (one Aider session each)

## Goal

Turn the Initializr build into the agreed setup: Gradle Kotlin DSL with a version catalog, a lean Testcontainers configuration, readable test output, and the Vietnam time zone for every JVM the build starts.

---

## Step 1: Kotlin DSL and version catalog

**Editable files:** `build.gradle`, `build.gradle.kts` (new), `settings.gradle`, `settings.gradle.kts` (new), `gradle/libs.versions.toml` (new)
**Read-only:** this task file

1. Convert `settings.gradle` to `settings.gradle.kts` with the same content (`rootProject.name = "scheduler"`).
2. Convert `build.gradle` to `build.gradle.kts`, keeping every dependency **except** `org.testcontainers:testcontainers-grafana`, which is removed.
3. Put the plugin versions (`org.springframework.boot` 4.1.1, `io.spring.dependency-management` 1.1.7) in `gradle/libs.versions.toml` under `[versions]` and `[plugins]`, and use `alias(libs.plugins.xxx)` in `build.gradle.kts`. Dependencies managed by Spring Boot stay without versions.
4. Keep the Java toolchain at 21.
5. Configure tests and `bootRun`:

   ```kotlin
   tasks.withType<Test> {
       useJUnitPlatform()
       jvmArgs("-Duser.timezone=Asia/Ho_Chi_Minh")
       testLogging {
           events("failed")
           exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
       }
   }

   tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
       jvmArgs("-Duser.timezone=Asia/Ho_Chi_Minh")
   }
   ```

6. Empty `build.gradle` and `settings.gradle`. The EM deletes them after the step: Gradle fails if both the Groovy and Kotlin files exist.

**Watch for (EM):** the dependency list matches the old file minus `testcontainers-grafana`; no version numbers in `build.gradle.kts`.

**Check (EM, after deleting the two Groovy files):** `.\gradlew.bat compileJava --console=plain` passes.

---

## Step 2: Lean Testcontainers configuration and line endings

**Editable files:** `src/test/java/com/keyloop/scheduler/TestcontainersConfiguration.java`, `.gitattributes`
**Read-only:** this task file, `build.gradle.kts`

1. In `TestcontainersConfiguration`, remove the `LgtmStackContainer` bean and its imports. Change the PostgreSQL image to `postgres:17`.
2. Replace `.gitattributes` with:

   ```text
   * text=auto
   gradlew text eol=lf
   *.sh text eol=lf
   *.bat text eol=crlf
   ```

**Test:** `/test .\gradlew.bat build --console=plain`

If the test log shows OpenTelemetry export connection warnings, leave them. T17 configures exporting.

## Done when

- `.\gradlew.bat build` passes with only `build.gradle.kts` and `settings.gradle.kts` present.
- No `grafana` references remain in the project.
