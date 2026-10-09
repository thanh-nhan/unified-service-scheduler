# T04: Runtime configuration, Dockerfile, and Compose

**Story:** E-01 (AC-E01.2, AC-E01.4) · **Type:** single task

## Goal

`docker compose up --build` starts PostgreSQL and the API with the `dev` fixture data, and the health endpoint reports UP.

## Read first

- `docs/user-stories/E-01-foundation.md`
- `docs/system-design/02-architecture.md` §2
- `src/main/resources/application.yml`, `src/main/resources/application-dev.yml`

## Files

| Action | File |
|---|---|
| Modify | `src/main/resources/application.yml` |
| Create | `Dockerfile` |
| Create | `compose.yaml` |
| Create | `.dockerignore` |

## Specification

**`application.yml`** adds:

```yaml
spring:
  threads:
    virtual:
      enabled: true
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/scheduler}
    username: ${DB_USER:scheduler}
    password: ${DB_PASSWORD:scheduler}
server:
  shutdown: graceful
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

**`Dockerfile`:** multi-stage. Build stage: a JDK 21 image, copy the Gradle wrapper and sources, run `./gradlew bootJar -x test --no-daemon`. Runtime stage: a JRE 21 image, non-root user, `ENV TZ=Asia/Ho_Chi_Minh`, `EXPOSE 8080`, `ENTRYPOINT ["java", "-jar", "/app/app.jar"]`.

**`compose.yaml`:**
- `db`: `postgres:17`, database/user/password `scheduler`, `TZ: Asia/Ho_Chi_Minh`, health check `pg_isready -U scheduler`, port `5432:5432`.
- `app`: builds the Dockerfile, `depends_on: db: condition: service_healthy`, environment `SPRING_PROFILES_ACTIVE=dev`, `DB_URL=jdbc:postgresql://db:5432/scheduler`, `TZ=Asia/Ho_Chi_Minh`, port `8080:8080`.

**`.dockerignore`:** `build/`, `.gradle/`, `.git/`, `docs/`, `.idea/`, `.vscode/`.

## Watch for (EM)

- The Initializr "Docker Compose support" dependency, if present, must not start a second database in tests.
- No credentials other than the local defaults.

## Done when (EM verifies by hand)

```powershell
docker compose up --build -d
curl http://localhost:8080/actuator/health        # {"status":"UP"} within 60 s
docker compose exec db psql -U scheduler -c "select count(*) from technician;"   # 5
docker compose down -v
```
