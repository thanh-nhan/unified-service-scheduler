# T19: Architecture rule tests (Should)

**Source:** `docs/system-design/02-architecture.md` §4 · **Type:** single task · **Drop first if time is short**

## Goal

Turn the module dependency rules into tests so future changes cannot quietly break them.

## Before starting (EM)

Give the agent the current ArchUnit JUnit 5 version (`com.tngtech.archunit:archunit-junit5`). The agent must not guess it.

## Read first

- `docs/system-design/02-architecture.md` §3, §4

## Files

| Action | File |
|---|---|
| Modify | `gradle/libs.versions.toml`, `build.gradle.kts` (test dependency) |
| Create | `src/test/java/com/keyloop/scheduler/ArchitectureTest.java` |

## Rules to encode

1. Classes in `..catalog..`, `..customer..`, `..common..` do not depend on `..availability..` or `..booking..`.
2. `..availability..` does not depend on `..booking..`.
3. Classes annotated `@RestController` do not depend on classes whose names end with `Repository` or `Queries`.
4. `SchedulingRules`, `AvailabilityCalculator`, `Slot`, `BusyInterval`, `DailyHours` do not depend on `org.springframework..` or `jakarta.persistence..`.
5. No class calls `java.time.LocalDateTime.now()` or `java.time.LocalDate.now()` without arguments.
6. No package cycles between top-level modules.

## Done when

`.\gradlew.bat test --tests "com.keyloop.scheduler.ArchitectureTest" --console=plain` passes. If a rule fails, report the violation to the EM instead of changing production code.
