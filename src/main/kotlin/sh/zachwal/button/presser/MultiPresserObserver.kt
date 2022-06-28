package sh.zachwal.button.presser

class MultiPresserObserver(
    private val observers: List<PresserObserver>
) : PresserObserver {
    override suspend fun pressed(presser: Presser) {
        observers.forEach {
            it.pressed(presser)
        }
    }

    override suspend fun released(presser: Presser) {
        observers.forEach {
            it.released(presser)
        }
    }

    override suspend fun disconnected(presser: Presser) {
        observers.forEach {
            it.disconnected(presser)
        }
    }
}
