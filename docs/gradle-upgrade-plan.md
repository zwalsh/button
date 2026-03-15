# Gradle 9 Upgrade Plan

## Overview

Four phases, each a standalone PR:

1. Rename `shared_html` package
2. Upgrade Gradle/Kotlin/ktlint + suppress all new rules
3. Enable new rules incrementally, one PR per rule
4. Enable configuration caching (can be folded into PR 2 or done separately)

---

## PR 1 — Rename `shared_html` package

Rename `sh.zachwal.button.shared_html` → `sh.zachwal.button.sharedhtml` (or `shared`).

- Move files: `src/main/kotlin/sh/zachwal/button/shared_html/` → `sharedhtml/`
- Update the ~15 import statements across the codebase
- No logic changes

This is done first so the ktlint `package-name` rule (which forbids underscores) does not need
to be suppressed in later PRs.

---

## PR 2 — Upgrade Gradle + Kotlin + ktlint, suppress all new rules

### Version bumps

| Thing | Before | After |
|---|---|---|
| Gradle | 7.1 | 9.4.0 |
| Kotlin plugin | 1.8.22 | 2.3.10 |
| ktlint plugin | 10.2.1 | 14.2.0 |

### `build.gradle.kts` changes

- Replace deprecated `kotlinOptions { jvmTarget = "17" }` with
  `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }`
- Update import: `KotlinCompile` → `org.jetbrains.kotlin.gradle.dsl.JvmTarget`
- Remove `val kotlinVersion` (no longer used)
- Remove `subprojects { apply(plugin = "org.jlleitschuh.gradle.ktlint") }` (no subprojects exist)

### Suppress all new ktlint rules

Run `./gradlew ktlintCheck` after the version bumps and collect every failing rule ID. Disable
each one in a new `.editorconfig` at the repo root. The result is a green build with zero code
changes — all new rules are parked for incremental review.

To find the rule IDs from the report:
```
./gradlew ktlintCheck 2>&1 | grep -oE '\(standard:[a-z-]+\)' | sort -u
```

Then for each rule ID `standard:foo`, add to `.editorconfig`:
```ini
[*.{kt,kts}]
ktlint_standard_foo = disabled
```

From the exploratory attempt, the rules that flagged violations were:
- `standard:indent` (many — deeply nested HTML builders)
- `standard:annotation`
- `standard:unary-op-spacing` (four `+\n"""` patterns)

There may be others; the grep above will catch everything.

---

## PR 3+ — Enable rules one at a time

For each rule, open a PR that:
1. Removes its `disabled` line from `.editorconfig`
2. Runs `./gradlew ktlintFormat` to auto-fix violations
3. Manually fixes anything `ktlintFormat` could not resolve
4. Verifies `./gradlew ktlintCheck` is green

Review and merge each one on its own merits. Skip rules you don't want.

Suggested order (easiest/least controversial first):
1. `standard:unary-op-spacing` — four small manual fixes (`+\n"""` → `+"""`)
2. `standard:annotation` — typically auto-fixed
3. `standard:indent` — largest diff; HTML builder nesting means many lines change; review carefully

---

## Notes

- Kotlin 2.x emits warnings about `@Inject`-annotated constructor parameters (`KT-73255`) in four
  files. These are informational only and can be addressed separately if desired.
- Configuration caching (`org.gradle.configuration-cache=true` in `gradle.properties`) can be
  added in PR 2 — it worked cleanly and adds no diff noise.
