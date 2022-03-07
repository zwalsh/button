package sh.zachwal.button.presser


class PresserManager : PresserObserver {
    private val pressers = mutableSetOf<Presser>()

    private var pressedCount = 0

    @Synchronized
    private suspend fun updateCountBy(update: Int) {
        pressedCount += update
        pressers.forEach { presser ->
            presser.updatePressingCount(pressedCount)
        }
    }

    override suspend fun pressed(presser: Presser) {
        updateCountBy(1)
    }

    override suspend fun released(presser: Presser) {
        updateCountBy(-1)
    }

    override suspend fun disconnected(presser: Presser) {
        pressers.remove(presser)
    }

    fun addPresser(presser: Presser) {
        pressers.add(presser)
    }
}
