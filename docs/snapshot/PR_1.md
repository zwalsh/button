# PR 1: Backend — Send Snapshot Message

## Goal

Implement and send the `Snapshot` server-to-client message from the backend. The frontend does not yet handle it (it will log "Unknown message type" in the console until PR 2).

## Files to Create

### `src/main/kotlin/sh/zachwal/button/presser/protocol/server/Snapshot.kt`

```kotlin
package sh.zachwal.button.presser.protocol.server

data class Snapshot(
    val count: Int,
    val names: List<String>
) : ServerMessage
```

## Files to Modify

### `src/main/kotlin/sh/zachwal/button/presser/protocol/server/ServerMessage.kt`

Add `Snapshot` to the `@JsonSubTypes` annotation:

```kotlin
@JsonSubTypes(
    JsonSubTypes.Type(CurrentCount::class),
    JsonSubTypes.Type(PersonPressing::class),
    JsonSubTypes.Type(PersonReleased::class),
    JsonSubTypes.Type(Snapshot::class),
)
```

### `src/main/kotlin/sh/zachwal/button/presser/Presser.kt`

1. Import `Snapshot`
2. Add a snapshot channel (capacity 1, DROP_LATEST — we only need the latest snapshot):
   ```kotlin
   private val snapshotChannel = Channel<Snapshot>(1, onBufferOverflow = BufferOverflow.DROP_LATEST)
   ```
3. Add a coroutine in `watchChannels()` to drain the channel:
   ```kotlin
   val outgoingSnapshot = scope.launch {
       for (snapshot in snapshotChannel) {
           sendServerMessage(snapshot)
       }
   }
   ```
   And join it alongside the other coroutines.
4. Add public method:
   ```kotlin
   suspend fun sendSnapshot(snapshot: Snapshot) {
       snapshotChannel.send(snapshot)
   }
   ```

### `src/main/kotlin/sh/zachwal/button/presser/PresserManager.kt`

1. Add a thread pool and coroutine scope (same pattern as `PressLogger`):
   ```kotlin
   private val threadPool = Executors.newSingleThreadExecutor(
       ThreadFactoryBuilder().setNameFormat("presser-manager-%d").build()
   )
   private val scope = CoroutineScope(threadPool.asCoroutineDispatcher() + SupervisorJob())
   ```
2. Add a private helper to build the current snapshot:
   ```kotlin
   private fun buildSnapshot(): Snapshot {
       val names = currentlyPressing.mapNotNull { it.contact?.name }
       return Snapshot(count = currentlyPressing.size, names = names)
   }
   ```
3. In `addPresser()`, send a snapshot to the new presser:
   ```kotlin
   suspend fun addPresser(presser: Presser) {
       pressers.add(presser)
       presser.updatePressingCount(currentlyPressing.count())
       presser.sendSnapshot(buildSnapshot())
   }
   ```
4. In `init {}`, launch the periodic snapshot broadcast:
   ```kotlin
   init {
       scope.launch {
           while (true) {
               delay(10_000)
               try {
                   val snapshot = buildSnapshot()
                   pressers.forEach { it.sendSnapshot(snapshot) }
               } catch (e: Exception) {
                   logger.error("Failed to send periodic snapshot", e)
               }
           }
       }
       Runtime.getRuntime().addShutdownHook(
           thread(start = false) { threadPool.shutdownNow() }
       )
   }
   ```

## Tests

Add backend tests covering:

1. **`addPresser` sends snapshot** — mock a `Presser`, call `addPresser()`, verify `sendSnapshot()` was called with correct count and names.
2. **Snapshot content** — when `currentlyPressing` contains a mix of authenticated and anonymous pressers, verify `buildSnapshot()` returns only the authenticated names and the total count.
3. **Periodic snapshot** — verify that after 10 seconds, all pressers receive a snapshot (can use `advanceTimeBy` or a fake clock, or just test the logic via `buildSnapshot`).

Look at existing tests in `src/test/kotlin/sh/zachwal/button/presser/` for patterns.

## Notes

- `PresserManager` is currently a plain `@Singleton` with no coroutine scope. The pattern to follow for adding one is `PressLogger` in `presshistory/PressLogger.kt`.
- The `@Inject constructor()` annotation will need to be added to `PresserManager` if not present, since Guice needs it for injection with the new dependencies — actually it already uses `@Singleton` with no-arg constructor, which Guice handles fine. Adding a thread pool in `init` is sufficient.
- No DI changes required.
