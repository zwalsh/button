package sh.zachwal.button.presser

interface PresserObserver {

    suspend fun pressed(presser: Presser)

    suspend fun released(presser: Presser)

    suspend fun disconnected(presser: Presser)
}
