# PR 2: DailyStatsService

## Goal
Implement `DailyStatsService` with its tests. No protocol or frontend changes.

## Files to create

### Service: `src/main/kotlin/sh/zachwal/button/presshistory/DailyStatsService.kt`
Sealed class for DB operations sent over the channel:
```kotlin
sealed class DbOp
data class NewPress(val date: LocalDate, val newPeak: Int?) : DbOp()  // newPeak non-null if peak was broken
data class NewPresser(val date: LocalDate, val presserId: String) : DbOp()
```

Implements `PresserObserver`. Thread-safe via `AtomicInteger` and `ConcurrentHashMap`-backed set
(observer callbacks arrive from the multi-threaded `presserDispatcher`).

Key behaviors:
- `initialize()`: loads today's row from `daily_stats`, loads `daily_pressers` for today into `uniquePresserIds`
- Observer callback `pressed(presser)`:
  1. Check midnight rollover (`LocalDate.now()` vs `trackingDate`); call `initialize()` if rolled
  2. Derive `presserId = presser.contact?.id?.toString() ?: presser.remoteHost`
  3. `totalPressCount.incrementAndGet()`
  4. `uniquePresserIds.add(presserId)` — if returns true, enqueue `NewPresser` DB op
  5. Enqueue `NewPress` DB op
- Observer callbacks `released(presser)` and `disconnected(presser)`: no-op
- `updatePeak(concurrentCount: Int)`: called by `PresserManager` after adding to `currentlyPressing`; uses `peakConcurrent.updateAndGet { max(it, concurrentCount) }`; enqueues DB op if peak changed
- `currentStats(): DailyStatsSnapshot` — returns snapshot of current in-memory values
- Background coroutine consumes `Channel<DbOp>` and executes DB writes

In-memory state (thread-safe):
```kotlin
private val uniquePresserIds: MutableSet<String> = ConcurrentHashMap.newKeySet()
private val peakConcurrent = AtomicInteger(0)
private val totalPressCount = AtomicInteger(0)
```

`DailyStatsSnapshot` is an in-memory value type (not the JDBI model) returned to callers:
```kotlin
data class DailyStatsSnapshot(val uniquePressers: Int, val peakConcurrent: Int, val totalPresses: Int)
```

Constructor injection: `DailyStatsDAO`, `DailyPressersDAO`.

## Files to modify
- `src/main/kotlin/sh/zachwal/button/db/JdbiModule.kt` — register `DailyStatsDAO`, `DailyPressersDAO`
- `src/main/kotlin/sh/zachwal/button/guice/ApplicationModule.kt` — bind `DailyStatsService` as eager singleton, call `initialize()`
- `src/main/kotlin/sh/zachwal/button/presser/PresserFactory.kt` — add `DailyStatsService` to `MultiPresserObserver` list, **before** `PresserManager` so stats are updated before the manager reads them

## Tests
`src/test/kotlin/sh/zachwal/button/presshistory/DailyStatsServiceTest.kt`
- Uses TestContainers PostgreSQL (follow pattern of existing integration tests)
- First press increments totalPresses, uniquePressers = 1
- Same presser again increments totalPresses only
- New presser increments uniquePressers (via daily_pressers insert)
- `updatePeak` with higher count updates peak; lower does not
- Concurrent calls to `pressed()` are safe (thread-safety test)
- `initialize()` reloads correctly after simulated restart

## Verification
```bash
./gradlew test --tests DailyStatsServiceTest
```
