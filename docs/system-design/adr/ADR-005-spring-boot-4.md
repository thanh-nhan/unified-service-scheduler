# ADR-005: Spring Boot 4.1

**Status:** Proposed · **Date:** 2026-10-08

## Context

The first plan was Spring Boot 3.x because the local coding model has seen far more Spring Boot 3 code. Checking current support status showed that Spring Boot 3.5 open-source support ended on 2026-06-30. Spring Boot 4.0 open-source support ends on 2026-12-31, and 4.1 on about 2027-07-31.

## Decision

Use **Spring Boot 4.1.x** (latest patch) on Java 21.

## Consequences

- The submission does not start life on an unsupported framework, and has the longest support window available.
- The coding model will tend to produce Spring Boot 3 code (old starter names, Jackson 2 packages, `@MockBean`). Mitigations: a Spring Boot 4 cheat sheet in `CONVENTIONS.md` (07), compile and test feedback in every task, and an initial spike that confirms dependency coordinates before feature work.
- Library compatibility (springdoc-openapi, Testcontainers 2) is verified in the spike.

## Alternatives considered

| Option | Why not |
|---|---|
| Spring Boot 3.5 | Best model familiarity, but out of open-source support; a poor signal for a production-minded design |
| Spring Boot 4.0 | Supported only until 2026-12-31; no advantage over 4.1 |
