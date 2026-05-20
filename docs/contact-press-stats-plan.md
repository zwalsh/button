# Plan: Admin Contact Press Stats Page

## Goal

Add a new admin page at `/admin/contact-press-stats` that shows each contact's press
count for a user-selected time range. The page lives in the existing admin Stats section.

## UX Requirements

- **Time ranges** (dropdown): Today, Last 7 Days, Last 30 Days, Year to Date, All Time
- **Table**: Contact name + press count; only contacts with ≥1 press in range shown
- **Sort**: Count descending
- **Default range**: Last 30 Days

## Architecture

### Data strategy

All press data up to (and including) yesterday UTC is materialized into the
`contact_press_counts` table by `ContactPressCountMaterializationTask`. Today's data
is only in the raw `press` table. The service combines both sources:

```
total = materialized (startDate..yesterday) + live press table (today midnight..now)
```

If the selected range starts today (i.e., "Today"), the materialized query is skipped
entirely. If it starts before today, both sources are queried and merged by `contactId`.

### New files

| File | Purpose |
|------|---------|
| `src/main/kotlin/sh/zachwal/button/admin/ContactPressStatsService.kt` | Hybrid query logic |
| `src/main/kotlin/sh/zachwal/button/admin/ContactPressStatsController.kt` | Route + HTML rendering |
| `src/test/kotlin/sh/zachwal/button/admin/ContactPressStatsServiceTest.kt` | Unit tests (MockK) |

### Modified files

| File | Change |
|------|--------|
| `src/main/kotlin/sh/zachwal/button/db/dao/PressDAO.kt` | Add `countByContactSince` |
| `src/test/kotlin/sh/zachwal/button/db/dao/PressDAOTest.kt` | Test for new DAO method |
| `src/main/kotlin/sh/zachwal/button/admin/AdminController.kt` | Add link under Stats section |

---

## Implementation detail

### Step 1 — New PressDAO method

Add to `PressDAO`:

```kotlin
@KeyColumn("contact_id")
@ValueColumn("count")
@SqlQuery(
    """
    select contact_id, count(*) as count
    from press
    where time >= :since and contact_id is not null
    group by contact_id
    """
)
fun countByContactSince(since: Instant): Map<Int, Long>
```

DAO test (`PressDAOTest`): create two contacts, insert presses at various times, assert that
`countByContactSince` returns correct per-contact totals and excludes anonymous presses
(`contact_id IS NULL`).

### Step 2 — TimeRange enum and ContactPressStatsService

Declare `TimeRange` inside or alongside the service (or in a dedicated small file in the
`admin` package):

```kotlin
enum class TimeRange(val label: String, val queryParam: String) {
    TODAY("Today", "today"),
    LAST_7_DAYS("Last 7 Days", "7d"),
    LAST_30_DAYS("Last 30 Days", "30d"),
    YEAR_TO_DATE("Year to Date", "ytd"),
    ALL_TIME("All Time", "all");

    companion object {
        fun fromParam(param: String?): TimeRange =
            values().find { it.queryParam == param } ?: LAST_30_DAYS
    }
}
```

Service logic outline:

```kotlin
@Singleton
class ContactPressStatsService @Inject constructor(
    private val contactDAO: ContactDAO,
    private val contactPressCountDAO: ContactPressCountDAO,
    private val pressDAO: PressDAO,
) {
    fun pressStats(range: TimeRange): List<ContactPressStat> {
        val today = LocalDate.now(ZoneOffset.UTC)
        val startDate: LocalDate = when (range) {
            TimeRange.TODAY        -> today
            TimeRange.LAST_7_DAYS  -> today.minusDays(7)
            TimeRange.LAST_30_DAYS -> today.minusDays(30)
            TimeRange.YEAR_TO_DATE -> LocalDate.of(today.year, 1, 1)
            TimeRange.ALL_TIME     -> LocalDate.ofEpochDay(0)
        }

        // Materialized counts (startDate..yesterday)
        val yesterday = today.minusDays(1)
        val materialized: Map<Int, Long> = if (startDate <= yesterday) {
            contactPressCountDAO
                .aggregateCountsByContact(startDate, yesterday)
                .mapValues { it.value.toLong() }
        } else {
            emptyMap()
        }

        // Today's raw counts
        val todayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant()
        val todayCounts: Map<Int, Long> = pressDAO.countByContactSince(todayStart)

        // Merge
        val allIds = (materialized.keys + todayCounts.keys).toSet()
        val contactsById = contactDAO.selectContacts().associateBy { it.id }

        return allIds.mapNotNull { id ->
            val contact = contactsById[id] ?: return@mapNotNull null
            val count = (materialized[id] ?: 0L) + (todayCounts[id] ?: 0L)
            ContactPressStat(contact, count)
        }
            .filter { it.count > 0 }
            .sortedByDescending { it.count }
    }
}

data class ContactPressStat(val contact: Contact, val count: Long)
```

Unit tests (use MockK, no real DB):

- `pressStats TODAY returns only live press counts` — mock materialized returning empty
  (or verify it's not called), mock `countByContactSince` with data
- `pressStats LAST_7_DAYS merges materialized and today counts correctly` — mock both
  sources with overlapping contact IDs, assert totals are summed
- `pressStats excludes contacts with zero count` — ensure a contact in the contacts list
  with nothing in either map doesn't appear
- `pressStats falls back to LAST_30_DAYS for unknown query param` — test
  `TimeRange.fromParam`

### Step 3 — Controller and admin link

**Controller** at `/admin/contact-press-stats`:

- Extract `range` query parameter (default `30d`)
- Call `service.pressStats(range)`
- Render page with `respondHtml`:
  - `headSetup()` + Bootstrap 4
  - `h1` "Contact Press Stats"
  - `form(method = FormMethod.get)` with a `select` dropdown for time range and a
    "Go" submit button; selected option reflects current range
  - `responsiveTable` with columns: **Name**, **Presses**; one row per `ContactPressStat`
  
Pattern reference: `AdminStatsController.kt` and `AdminContactController.kt`.

**Admin link** — in `AdminController.kt`, inside the Stats `ul` block, add:

```kotlin
li {
    a(href = "/admin/contact-press-stats") {
        +"Contact Press Stats"
    }
}
```

Register the new controller in Guice (follow the existing `@Controller` + `ControllerCreator`
pattern — no manual registration needed, just the `@Controller` annotation).

---

## Commit / checkpoint sequence

The implementing agent **must pause and ask the user for feedback before each commit**.

### Commit 1 — DAO layer  
**Files**: `PressDAO.kt`, `PressDAOTest.kt`  
**TDD steps**:
1. Write failing test for `countByContactSince` in `PressDAOTest`
2. Run `./gradlew test --tests PressDAOTest` → expect failure
3. Add method to `PressDAO`
4. Run tests → expect green
5. **Pause. Ask user to review the DAO change before committing.**
6. Commit: `Add countByContactSince to PressDAO`

### Commit 2 — Service layer  
**Files**: `ContactPressStatsService.kt`, `ContactPressStatsServiceTest.kt`  
**TDD steps**:
1. Write failing unit tests for `ContactPressStatsService`
2. Run tests → expect failure
3. Implement service and `TimeRange` enum
4. Run tests → expect green
5. **Pause. Ask user to review the service before committing.**
6. Commit: `Add ContactPressStatsService with hybrid materialized/live query`

### Commit 3 — Controller and UI  
**Files**: `ContactPressStatsController.kt`, `AdminController.kt`  
**Steps**:
1. Implement controller and add admin index link
2. Run `./gradlew build` → expect green
3. **Pause. Ask user to run the app and review the page UI before committing.**
4. Commit: `Add admin contact press stats page`

### Final step — Push branch and open PR  
After user approves all commits, push the branch and open a GitHub PR against `main`.
Use `gh pr create` with a descriptive title and summary body.

---

## Notes for the implementing agent

- Use `ZoneOffset.UTC` for all date calculations (consistent with the rest of the codebase).
- Do not add error handling for impossible cases (e.g., unknown contact IDs in press data);
  `mapNotNull` naturally handles FK mismatches.
- The `@Controller` annotation + `ControllerCreator` auto-discovery means no manual Guice
  binding is needed for the new controller — just ensure the class has `@Controller` and is
  in a package scanned by the application module.
- Check `JdbiModule.kt` to confirm `ContactPressCountDAO` is already bound (it is, per
  exploration); no new Guice binding is needed for DAOs.
- The service should be `@Singleton` (stateless but injected).
- Keep HTML generation consistent with existing admin pages: `headSetup()`,
  `responsiveTable()`, Bootstrap 4 classes.
