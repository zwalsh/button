# Copilot Instructions for this Repository

Last updated: 2025-12-06T18:51:18.474Z

## Purpose
A Kotlin/Ktor web app where users press a shared button and see how many are pressing concurrently. Real-time via WebSockets; persists press history; optionally sends SMS notifications; includes admin features and seasonal visuals.

## Architecture overview
- Server: Ktor with DI via Guice modules. Main entrypoint: `sh.zachwal.button.AppKt`.
- Real-time: WebSockets manage press state (protocol messages in `presser/protocol/*`), coordinated by `PresserManager` and observers (`PressHistoryObserver`, `MultiPresserObserver`).
- HTTP/controllers: Home, Users, Phone, Contact, Admin, Wrapped; controller creation via custom `Controller` annotation and `ControllerCreator`.
- Auth/sessions/roles: `SessionService`, `SessionAuth`, `FormAuth`, principals (`UserSessionPrincipal`, `ContactSessionPrincipal`), role checks (`RoleAuthorization` / `AuthorizedRoute`).
- Persistence: PostgreSQL via HikariCP + JDBI DAOs and models; migrations with Liquibase JSON in `/db` and `migrate.sh`.
- Static assets: maintained in `frontend/src/main` (HTML/CSS/JS). Images stay in JVM resources (`src/main/resources/static/special`). The Gradle build copies `frontend/src/main` into `src/main/resources/static` at build time (see `copyFrontend` task).
- Observability: Logback, Sentry; Umami analytics config.

## Key directories and files
- `src/main/kotlin/sh/zachwal/button/` core server code (controllers, services, features, config, auth, roles).
- `src/main/resources/` config and static assets (`application.conf`, `hikari.properties`, `logback.xml`, `static/`).
- `frontend/src/main` static CSS / JS.
- `db/` Liquibase migrations (`changelog.json`, numbered change sets, `migrate.sh`, `liquibase.properties`).
- `build.gradle.kts`, `settings.gradle.kts` build configuration.
- `Jenkinsfile` CI/CD pipeline.
- `button.service`, `testbutton.service` systemd units; `button.env` runtime DB env vars.
- `docs/` deployment and protocol notes.
- `src/test/kotlin/` tests (JUnit5 + MockK + Truth).

## External integrations
- Twilio SMS (`sms/TwilioMessagingService.kt`, `ControlledContactMessagingService.kt`).
- Sentry error reporting (`config/SentryConfig.kt`).
- Umami analytics (`config/UmamiConfig.kt`).
- GitHub Status API updates (Jenkinsfile `setBuildStatus`).

## Build, test, and run
- Build/test locally:
  - `./gradlew assemble testClasses` to compile
  - `./gradlew ktlintCheck` to lint
  - `./gradlew build` to test
  - Frontend tests: `npm --prefix frontend install && npm --prefix frontend test`.
- Run (distribution tar produced by Gradle):
  - `./gradlew assemble` -> `build/distributions/button.tar`
  - Unpack and run `button/bin/button` (systemd units reference this layout).
- Tests: JUnit5 (`./gradlew test`); WebSocket tests use Ktor clients.

## Deployment & migrations
- CI/CD (Jenkinsfile): assembles, lints, tests, and releases to a test environment (and production on merges to main).
- Liquibase Migrations: run automatically via CI/CD 
- Runtime env: systemd units read `EnvironmentFile` (`/home/{button|testbutton}/button.env`) with `DB_USER`, `DB_PASSWORD`, `DB_NAME`.

## Coding patterns & conventions
- Controllers via `@Controller` and `ControllerCreator`; Ktor routing + role middleware (`RoleAuthorization`).
- DI modules: `ApplicationModule`, `ConfigModule`, `HikariModule`, `JdbiModule`, `JacksonModule`, `MessagingModule`.
- Data access with JDBI SQLObject; some DAO methods use external SQL in `src/main/resources/sh/zachwal/button/db/dao/*`.
- Protocol messages are Kotlin data classes under `presser/protocol/*` with JSON via Jackson.
- Prefer service boundaries (e.g., `PhoneBookService`, `ContactNotifier`, `WrappedService`) and observers for side effects.

## Guardrails
- Do not commit secrets. Use environment variables or secret stores.
- Maintain backward-compatible WebSocket protocol types; update clients alongside server changes.
- Keep migrations idempotent and versioned.
- Respect role-based authorization for admin routes.
- Rate/volume control when sending SMS to avoid costs; use `ControlledContactMessagingService` for limits.
