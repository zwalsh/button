package sh.zachwal.button.presser

/**
 * Observer interface for reacting to Presser state changes (pressed, released, disconnected).
 *
 * Implementations (like PresserManager, MultiPresserObserver) coordinate global state and broadcast updates.
 */
interface PresserObserver {

    suspend fun pressed(presser: Presser)

    suspend fun released(presser: Presser)

    suspend fun disconnected(presser: Presser)
}
