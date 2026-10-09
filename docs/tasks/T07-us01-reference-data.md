# T07: Browse dealerships and service types

**Story:** US-01 (AC-01.1 to AC-01.3) · **Parts:** A (tests), B (implementation)

## Read first (both parts)

- `docs/user-stories/US-01-reference-data.md`
- `src/test/java/com/keyloop/scheduler/support/IntegrationTest.java`, `support/Fixture.java`

---

## Part A: acceptance tests

**Create:** `src/test/java/com/keyloop/scheduler/acceptance/ReferenceDataAcceptanceTest.java` (extends `IntegrationTest`).

- One test per AC. `@DisplayName` starts with the AC ID.
- Use `mockMvc` and `jsonPath` to assert the exact order, counts, and fields in the AC.
- AC-01.2: assert the items have no `requiredSkill` / `requiredBayType` field (`jsonPath("$[0].requiredSkill").doesNotExist()`).
- AC-01.3: delete all service types in the test with `jdbc` first (appointments are empty, so no foreign key blocks it).

**Done when:** the class compiles; tests fail with 404 (endpoints do not exist yet). Run:
`.\gradlew.bat test --tests "com.keyloop.scheduler.acceptance.ReferenceDataAcceptanceTest" --console=plain`

**Then (EM):** review each test against its AC, add `// FROZEN: approved by EM <date>` as line 1, commit.

---

## Part B: implementation

**Files:**

| Action | File |
|---|---|
| Create | `catalog/CatalogService.java`, `catalog/CatalogController.java` |
| Create | `catalog/dto/DealershipResponse.java`, `catalog/dto/OpeningHoursResponse.java`, `catalog/dto/ServiceTypeResponse.java` |
| Modify | repositories from T06, only to add sorted finder methods |

**Specification:**
- `GET /api/v1/dealerships`: sorted by name; `openingHours` sorted MONDAY → SUNDAY, closed days omitted; times as `"08:00"`.
- `GET /api/v1/service-types`: sorted by name; fields `id`, `code`, `name`, `durationMinutes` only.
- Load opening hours without N+1 queries (fetch join or entity graph).

**Watch for (EM):** DTOs are records; no entity returned from the controller; no required skill or bay type in the response.

**Done when:** the frozen `ReferenceDataAcceptanceTest` passes unchanged.
