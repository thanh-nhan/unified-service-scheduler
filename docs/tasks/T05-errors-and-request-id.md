# T05: Error model, date-time parsing, and request ID

**Stories:** GR-01, GR-10, GR-11 in `00-conventions.md`; E-02 (AC-E02.1, AC-E02.2) · **Type:** single task, two Aider sessions

## Goal

Build the shared error handling every endpoint will use, strict local date-time parsing, and the `X-Request-Id` filter.

## Read first

- `docs/user-stories/00-conventions.md` (Global rules and Error code catalogue)
- `docs/system-design/06-api.md` §3
- `docs/system-design/08-observability.md` §1

## Files

| Action | File |
|---|---|
| Create | `src/main/java/com/keyloop/scheduler/common/error/ErrorCode.java` |
| Create | `src/main/java/com/keyloop/scheduler/common/error/ApiException.java` |
| Create | `src/main/java/com/keyloop/scheduler/common/error/GlobalExceptionHandler.java` |
| Create | `src/main/java/com/keyloop/scheduler/common/time/LocalDateTimes.java` |
| Create | `src/main/java/com/keyloop/scheduler/common/web/RequestIdFilter.java` |
| Create | `src/test/java/com/keyloop/scheduler/common/ErrorHandlingTest.java` |
| Create | `src/test/java/com/keyloop/scheduler/common/TestErrorController.java` (test-only controller) |
| Create | `src/test/java/com/keyloop/scheduler/common/LocalDateTimesTest.java` |
| Create | `src/test/java/com/keyloop/scheduler/common/RequestIdTest.java` |

## Sessions

| Session | Editable files | Test |
|---|---|---|
| 1 | `ErrorCode`, `ApiException`, `GlobalExceptionHandler`, `TestErrorController`, `ErrorHandlingTest` | `/test .\gradlew.bat test --tests "com.keyloop.scheduler.common.ErrorHandlingTest" --console=plain` |
| 2 | `LocalDateTimes`, `LocalDateTimesTest`, `RequestIdFilter`, `RequestIdTest` (session 1 files read-only) | the "Done when" command below |

## Specification

**`ErrorCode`:** an enum with every code from the catalogue. Each has an `HttpStatus` and a customer-readable default `detail`, e.g. `NO_QUALIFIED_TECHNICIAN_AVAILABLE(CONFLICT, "No technician is available for the selected time. Please choose another time.")`.

**`ApiException`:** `RuntimeException` with an `ErrorCode` and an optional detail override.

**`GlobalExceptionHandler`** (`@RestControllerAdvice`): returns `ProblemDetail` with extra properties `code` and `requestId` (from MDC). Mapping:

| Exception | Response |
|---|---|
| `ApiException` | its code's status and `code` |
| `MethodArgumentNotValidException`, `HandlerMethodValidationException` | 400 `VALIDATION_FAILED` with `errors: [{field, message}]` |
| `HttpMessageNotReadableException` (bad JSON) | 400 `VALIDATION_FAILED` |
| `MethodArgumentTypeMismatchException` (e.g. malformed UUID or date in path/query) | 400 `VALIDATION_FAILED` with an `errors` entry for that parameter |
| `MissingServletRequestParameterException` | 400 `VALIDATION_FAILED` with an `errors` entry |
| `MissingRequestHeaderException` for `Idempotency-Key` | 400 `IDEMPOTENCY_KEY_MISSING` |
| Any other exception | 500, generic detail, no internal message; log at ERROR with stack trace |

**`LocalDateTimes`:**
- `LocalDateTime parse(String field, String value)` uses `LocalDateTime.parse(value)` (ISO local date-time). On failure throws `ApiException(VALIDATION_FAILED)` with an `errors` entry for `field`. It must reject `2026-10-12T09:30:00+07:00` and `2026-10-12T02:30:00Z`, and accept `2026-10-12T09:15:30`.
- `String format(LocalDateTime)` returns `yyyy-MM-dd'T'HH:mm:ss` (always with seconds).

**`RequestIdFilter`** (`OncePerRequestFilter`, highest precedence): uses the incoming `X-Request-Id` if it matches `^[A-Za-z0-9._-]{1,64}$`, otherwise generates a UUID; puts it in MDC as `requestId`; sets the response header; clears MDC in `finally`.

## Tests

- `ErrorHandlingTest` (`@WebMvcTest` with the test controller, or extends `IntegrationTest`): one test per mapping row. In session 1 the `requestId` property may be null.
- `RequestIdTest` (session 2):
  - `AC-E02.1`: request with `X-Request-Id: abc-123` gets it back; request without it gets a UUID.
  - `AC-E02.2`: a problem response's `requestId` equals its `X-Request-Id` header.
- `LocalDateTimesTest`: plain unit test for the accept and reject cases above.

## Watch for (EM)

- Status codes come only from `ErrorCode`.
- The 500 response does not leak exception messages.
- MDC is cleared even when the request throws.

## Done when

`.\gradlew.bat test --tests "com.keyloop.scheduler.common.*" --console=plain` passes.
