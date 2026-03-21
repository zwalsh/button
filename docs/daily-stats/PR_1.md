# PR 1: DB Migrations

## Goal
Create the two new tables, with their DAOs and tests.

## Files to create

### `db/13_create_daily_stats.json`
```sql
CREATE TABLE daily_stats (
    date              DATE NOT NULL,
    peak_concurrent   INT  NOT NULL DEFAULT 0,
    total_press_count INT  NOT NULL DEFAULT 0,
    CONSTRAINT pk_daily_stats PRIMARY KEY (date)
);
```

### `db/14_create_daily_pressers.json`
```sql
CREATE TABLE daily_pressers (
    date       DATE NOT NULL,
    presser_id TEXT NOT NULL,
    CONSTRAINT pk_daily_pressers PRIMARY KEY (date, presser_id)
);
```


### Model: `src/main/kotlin/sh/zachwal/button/db/jdbi/DailyStatsRow.kt`
```kotlin
data class DailyStatsRow(
    val date: LocalDate,
    val peakConcurrent: Int,
    val totalPressCount: Int,
)
```

### DAO: `src/main/kotlin/sh/zachwal/button/db/dao/DailyStatsDAO.kt`
```kotlin
interface DailyStatsDAO {
    @SqlQuery("SELECT * FROM daily_stats WHERE date = :date")
    fun findByDate(@Bind("date") date: LocalDate): DailyStatsRow?

    @SqlUpdate("INSERT INTO daily_stats (date) VALUES (:date) ON CONFLICT DO NOTHING")
    fun ensureRow(@Bind("date") date: LocalDate)

    @SqlUpdate("UPDATE daily_stats SET total_press_count = total_press_count + 1 WHERE date = :date")
    fun incrementTotalPresses(@Bind("date") date: LocalDate)

    @SqlUpdate("UPDATE daily_stats SET peak_concurrent = GREATEST(peak_concurrent, :peak) WHERE date = :date")
    fun updatePeakIfHigher(@Bind("date") date: LocalDate, @Bind("peak") peak: Int)
}
```

### DAO: `src/main/kotlin/sh/zachwal/button/db/dao/DailyPressersDAO.kt`
```kotlin
interface DailyPressersDAO {
    @SqlQuery("SELECT presser_id FROM daily_pressers WHERE date = :date")
    fun findByDate(@Bind("date") date: LocalDate): List<String>

    // Returns true if the row was inserted (presser is new for this date)
    @SqlQuery("""
        INSERT INTO daily_pressers (date, presser_id) VALUES (:date, :presserId)
        ON CONFLICT DO NOTHING
        RETURNING presser_id
    """)
    fun insertIfAbsent(@Bind("date") date: LocalDate, @Bind("presserId") presserId: String): String?
}
```

## Files to modify
- `db/changelog.json` — add both new changeset entries

## Verification
Run migrations against test DB; confirm both tables exist with correct constraints.

## Tests
Write integration tests of the DAOs using existing TestContainers PostgreSQL patterns.
