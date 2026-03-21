# Daily Stats Feature Plan

## Problem
Users who join late see only themselves pressing the button. Showing daily stats creates
engagement even for late arrivals.

## Goal
Display stats under the "BUTTON PRESSERS: N" heading:
> 4 pressers today · peak 2 · 17 presses

## Storage
Two new tables:
- `daily_stats(date DATE PK, peak_concurrent INT, total_press_count INT)` — one row per day
- `daily_pressers(date DATE, presser_id TEXT, PRIMARY KEY (date, presser_id))` — one row per unique presser per day

Presser identity: `contact_id.toString()` if authenticated, else `remoteHost`.

## DailyStatsService
Implements `PresserObserver` and maintains thread-safe in-memory state for the current day:
- `uniquePresserIds: ConcurrentHashMap.newKeySet<String>()` — loaded from `daily_pressers` on startup
- `peakConcurrent: AtomicInteger`, `totalPressCount: AtomicInteger` — loaded from `daily_stats` on startup
- `trackingDate: LocalDate` — if rolled past midnight, re-initialize from DB before proceeding

Uses `AtomicInteger` and `ConcurrentHashMap`-backed set for thread safety, since observer
callbacks arrive from the multi-threaded `presserDispatcher`.

Wired into `MultiPresserObserver` (before `PresserManager`) so that stats are updated before
`PresserManager` reads them. `PresserManager` is also injected with `DailyStatsService` directly
so it can call `updatePeak(concurrentCount)` (after adding to `currentlyPressing`) and
`currentStats()` (to build and broadcast the `DailyStats` message).

DB writes are queued via a `Channel<DbOp>` consumed by a single background coroutine:
- New presser: `INSERT INTO daily_pressers ... ON CONFLICT DO NOTHING`
- Every press: `UPDATE daily_stats SET total_press_count = total_press_count + 1`
- New peak: `UPDATE daily_stats SET peak_concurrent = GREATEST(peak_concurrent, :n)`

## Protocol
New `DailyStats` ServerMessage:
```json
{ "type": "DailyStats", "uniquePressers": 4, "peakConcurrent": 2, "totalPresses": 17 }
```
Sent on every press, on new connection, and bundled into `Snapshot`.

## Implementation phases

| PR | Scope |
|----|-------|
| PR 1 | DB migrations only: `daily_stats` + `daily_pressers` tables |
| PR 2 | `DailyStatsService`, DAOs, tests |
| PR 3 | Protocol message, `PresserManager` integration, `Snapshot` extension |
| PR 4 | Frontend display |
