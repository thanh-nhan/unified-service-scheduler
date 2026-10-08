# E-01: Project foundation (enabler)

**Priority:** Must · **Depends on:** none · **Status:** Draft

## Story

**As** the engineering team
**I want** a runnable, testable project skeleton with agreed conventions
**So that** every story is built the same way and can be verified automatically

## Scope

Java 21, Spring Boot 3.x (the line the coding model knows best), Maven Wrapper, PostgreSQL 16, Flyway, Testcontainers, Docker Compose, Spring Boot Actuator.

## Acceptance criteria

**AC-E01.1: Build and test from a clean clone**
Given a clean clone, with JDK 21 and Docker installed
When `./mvnw verify` runs
Then the build succeeds
And integration tests run against PostgreSQL started by Testcontainers (no H2 or other embedded database)

**AC-E01.2: Run with one command**
Given a clean clone, with Docker installed
When `docker compose up --build` runs
Then within 60 seconds `GET http://localhost:8080/actuator/health` returns 200 with `"status": "UP"`

**AC-E01.3: Schema managed by migrations**
Given an empty database
When the application starts
Then Flyway creates the full schema defined in the approved design
And Hibernate runs with `ddl-auto=validate`, never `create` or `update`

**AC-E01.4: Seed data in dev only**
Given the `dev` profile (the default in Docker Compose)
When the application starts
Then the fixture dataset from `00-conventions.md` exists with exactly those IDs
And with any other profile, no seed data is loaded automatically

**AC-E01.5: Reusable test fixture**
Given any integration test
When it starts
Then the database contains exactly the fixture dataset and no appointments
And changes made by one test are not visible to the next

**AC-E01.6: Controllable clock**
Given a test that overrides the `Clock` bean with a fixed instant
When application code asks for the current time
Then it receives that fixed instant

## Notes for implementer

- Package structure, naming, and libraries are defined in `AGENTS.md`.
- Do not add any business endpoints in this story.
