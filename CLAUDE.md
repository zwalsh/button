## Project Overview

Button is a Kotlin/Ktor web app where users press a shared button and see how many people are pressing concurrently in real-time via WebSockets. It persists press history in PostgreSQL, optionally sends SMS notifications via Twilio, and includes admin features and seasonal visuals.

Live site: https://button.zachwal.sh

## Build, Test, and Run Commands

```bash
# Build
./gradlew assemble testClasses        # Compile code
./gradlew build                       # Full build with tests
./gradlew copyFrontend                # Copy frontend assets to resources

# Backend tests (JUnit5 + MockK + Truth)
./gradlew test                        # Run all tests
./gradlew test --tests ClassName      # Run specific test class

# Frontend tests (Vitest + jsdom)
npm ci --prefix frontend              # Install deps
npm --prefix frontend test            # Run tests

# Linting
./gradlew ktlintCheck                 # Lint Kotlin
./gradlew ktlintFormat                # Auto-format Kotlin

# Run locally (requires DB env vars)
./gradlew run

# Distribution
./gradlew assemble                    # Creates build/distributions/button.tar
```

## Architecture

**Backend (Kotlin/Ktor)**:
- Main entrypoint: `sh.zachwal.button.AppKt`
- DI via Guice modules: `ApplicationModule`, `ConfigModule`, `HikariModule`, `JdbiModule`, `JacksonModule`, `MessagingModule`
- Real-time: WebSockets managed by `PresserManager` with observer pattern (`PressHistoryObserver`, `MultiPresserObserver`)
- Controllers: Custom `@Controller` annotation with reflection-based routing via `ControllerCreator`. Main controllers: Home, Users, Phone, Contact, Admin, Wrapped.
- Auth: Multi-session system (`SessionService`) with `FormAuth` and `SessionAuth` middleware. Principals: `UserSessionPrincipal`, `ContactSessionPrincipal`. Role-based authorization via `RoleAuthorization` and `AuthorizedRoute`.
- Persistence: PostgreSQL with HikariCP + JDBI DAOs; migrations via Liquibase JSON in `/db`
- Service boundaries: Key services include `PhoneBookService`, `ContactNotifier`, `WrappedService`

**Frontend**:
- Static HTML/CSS/JS in `frontend/src/main/` (copied to resources at build time)
- WebSocket client in `js/net/socket.js` with auto-reconnection
- Protocol messages: Kotlin data classes in `presser/protocol/*`, JSON-serialized via Jackson

## Key Directories

- `src/main/kotlin/sh/zachwal/button/` - Server code (controllers, services, auth, features, config)
- `src/main/resources/` - Config files (`application.conf`, `hikari.properties`, `logback.xml`, `static/`)
- `frontend/src/main/` - Static CSS/JS (images stay in `src/main/resources/static/special`)
- `frontend/src/__tests__/` - Frontend tests
- `db/` - Liquibase migrations (`changelog.json`, numbered change sets, `migrate.sh`, `liquibase.properties`)
- `src/test/kotlin/` - Backend tests
- `docs/` - Deployment and protocol documentation
- `build.gradle.kts`, `settings.gradle.kts` - Gradle build configuration
- `Jenkinsfile` - CI/CD pipeline configuration
- `button.service`, `testbutton.service` - Systemd service units

## Deployment & CI/CD

- **CI**: Jenkinsfile runs build, lint, and test, then sets a GitHub commit status (`continuous-integration/jenkins`)
- **CD**: Server-side `scripts/deploy.sh`, triggered by systemd timers (`button-deploy.timer`, `testbutton-deploy.timer`), polls for a new green commit and deploys it
- **Environments**: `button` (production) deploys `origin/main`; `testbutton` deploys the tip of the most-recently-updated remote branch
- **Migrations**: `db/migrate.sh` (Liquibase) is run by `deploy.sh` on each deploy
- **Runtime Config**: Systemd units read environment variables from `EnvironmentFile` (`~/button.env`) containing `DB_USER`, `DB_PASSWORD`, `DB_NAME`, etc.
- **Distribution**: `./gradlew assemble` creates `build/distributions/button.tar`; unpacked into `~/releases/$SHA/` and symlinked to `~/releases/current`
- **Credentials**: Deploy key at `~/.ssh/deploy_key` for git access; fine-grained PAT at `~/.github_token` for reading CI status

## Coding Patterns

**Controllers**: Use `@Controller` annotation. Methods with signature `fun methodName(routing: Routing)` are auto-discovered and invoked by `ControllerCreator`.

**Database Access**: JDBI SQLObject pattern with DAO interfaces. Some DAOs use external SQL files in `src/main/resources/sh/zachwal/button/db/dao/*.sql`.

**Side Effects**: Use observer pattern (e.g., `PressHistoryObserver`) rather than inline side effects in services.

**Testing**: Backend uses TestContainers with PostgreSQL for DAO/DB integration tests. Frontend uses Vitest + jsdom. For HTTP-layer tests (form parsing, redirects, auth gating), use `withContactTestApp` from `src/test/kotlin/sh/zachwal/button/testing/KtorTestApp.kt` — see the KDoc there for when to reach for it. Prefer unit tests (mockk) for service logic; keep Ktor integration tests to one file per controller, testing only what cannot be exercised without a real request cycle.

## External Integrations

- **Twilio**: SMS messaging via `sms/TwilioMessagingService.kt`, rate-limited by `ControlledContactMessagingService.kt`
- **Sentry**: Error reporting configured in `config/SentryConfig.kt` (Kotlin + JS)
- **Umami**: Analytics configuration in `config/UmamiConfig.kt`
- **GitHub Status API**: Build status updates via Jenkinsfile `setBuildStatus`

## Guardrails

- Do not commit secrets; use environment variables
- Maintain backward-compatible WebSocket protocol; update clients alongside server
- Keep migrations idempotent and versioned
- Respect role-based authorization on admin routes
- Use `ControlledContactMessagingService` for SMS rate limiting
