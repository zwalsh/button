# PR 3: Protocol + PresserManager Integration

## Goal
Add the `DailyStats` ServerMessage and wire `DailyStatsService` into `PresserManager` for broadcasting.

## Files to create

### `src/main/kotlin/sh/zachwal/button/presser/protocol/server/DailyStats.kt`
```kotlin
data class DailyStats(
    val uniquePressers: Int,
    val peakConcurrent: Int,
    val totalPresses: Int,
) : ServerMessage
```

## Files to modify

### `src/main/kotlin/sh/zachwal/button/presser/protocol/server/ServerMessage.kt`
Add `DailyStats` to the `@JsonSubTypes` annotation:
```kotlin
@JsonSubTypes(
    JsonSubTypes.Type(CurrentCount::class),
    JsonSubTypes.Type(PersonPressing::class),
    JsonSubTypes.Type(PersonReleased::class),
    JsonSubTypes.Type(Snapshot::class),
    JsonSubTypes.Type(DailyStats::class),  // new
)
```

### `src/main/kotlin/sh/zachwal/button/presser/protocol/server/Snapshot.kt`
Add field:
```kotlin
data class Snapshot(
    val count: Int,
    val names: List<String>,
    val dailyStats: DailyStats,  // new
) : ServerMessage
```

### `src/main/kotlin/sh/zachwal/button/presser/Presser.kt`
Add channel and send helper for `DailyStats` (mirroring existing `countUpdateChannel` pattern).

### `src/main/kotlin/sh/zachwal/button/presser/PresserManager.kt`
- Inject `DailyStatsService`
- `addPresser()`: send `dailyStatsService.currentStats()` as `DailyStats` message to new presser
- `pressed()` (after existing `currentlyPressing.add(presser)`):
  1. Call `dailyStatsService.updatePeak(currentlyPressing.size)`
  2. Read `dailyStatsService.currentStats()` and broadcast as `DailyStats` message to all pressers
  (Note: `DailyStatsService.pressed()` has already run via `MultiPresserObserver` before
  `PresserManager.pressed()`, so totalPresses and uniquePressers are already updated.)
- `buildSnapshot()`: include `dailyStatsService.currentStats()` in `Snapshot`

## Verification
```bash
./gradlew build
```
Manual: open two browser tabs, press in one — verify WS frames contain `DailyStats` messages in both.
