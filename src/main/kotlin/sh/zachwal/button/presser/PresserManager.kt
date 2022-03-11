package sh.zachwal.button.presser

import org.slf4j.LoggerFactory

class PresserManager : PresserObserver {

    private val logger = LoggerFactory.getLogger(PresserManager::class.java)
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
        logger.info("Presser disconnected")
        pressers.remove(presser)
    }

    suspend fun addPresser(presser: Presser) {
        pressers.add(presser)
        presser.updatePressingCount(pressedCount)
    }
}
