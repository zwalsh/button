# Frontend

This project keeps its frontend source and tests in the `frontend/` directory. Images live in JVM resources
(src/main/resources/static/)

Layout:

- frontend/package.json  — npm project for tests and tooling (Vitest/jsdom, etc.)
- frontend/src/main/*    — JS/CSS/HTML sources which are copied into the JVM resources during build
- frontend/src/__tests__ — Vitest test files

How to run:

- Install dev deps: npm --prefix frontend install
- Run frontend tests: npm --prefix frontend test
- Copy frontend into server resources (Gradle): ./gradlew copyFrontend
- Full build: ./gradlew assemble (assemble depends on copyFrontend)
