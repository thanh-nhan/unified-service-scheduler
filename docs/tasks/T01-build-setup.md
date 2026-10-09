# T01: Build setup and Spring Boot 4 check

**Story:** E-01 (AC-E01.1) · **Type:** single task

## Goal

Make the Initializr build ready for the project: a version catalog, readable test output, consistent line endings, and a confirmed Spring Boot 4 dependency set.

## Read first

- `build.gradle.kts`, `settings.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties`
- `docs/system-design/07-technology-choices.md` (only the "Spring Boot 4 notes" table)

## Files

| Action | File |
|---|---|
| Create | `gradle/libs.versions.toml` |
| Modify | `build.gradle.kts` |
| Create | `.gitattributes` |
| Modify | `.gitignore` (only if `build/`, `.gradle/`, `.idea/`, `.vscode/` are missing) |

## Steps

1. Move every explicitly versioned dependency and plugin into `gradle/libs.versions.toml`, and reference them as `libs.xxx` in `build.gradle.kts`. Do not change versions. Dependencies managed by the Spring Boot plugin stay without versions.
2. Configure the Java toolchain to 21.
3. Configure `tasks.test`:
   - `useJUnitPlatform()`
   - `testLogging { events("failed"); exceptionFormat = TestExceptionFormat.SHORT }`
4. Create `.gitattributes`:

   ```text
   * text=auto
   gradlew text eol=lf
   *.sh text eol=lf
   *.bat text eol=crlf
   ```

5. Check the dependency list against `.clinerules` §4. Report any Spring Boot 3 style coordinates you find. Do not change them without approval.

## Watch for (EM)

- No version numbers left in `build.gradle.kts`.
- No dependencies added or removed.

## Done when

`.\gradlew.bat build --console=plain` passes.
