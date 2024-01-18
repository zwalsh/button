package sh.zachwal.button.presser

import com.google.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

@Singleton
class PresserManager : PresserObserver {

    private val logger = LoggerFactory.getLogger(PresserManager::class.java)

    private val pressers: MutableSet<Presser> = ConcurrentHashMap.newKeySet()
    private val currentlyPressing: MutableSet<Presser> = ConcurrentHashMap.newKeySet()

    private suspend fun update() {
        val pressingCount = currentlyPressing.count()
        logger.info("Pressing count now $pressingCount")
        pressers.forEach { presser ->
            presser.updatePressingCount(pressingCount)
        }
    }

    override suspend fun pressed(presser: Presser) {
        currentlyPressing.add(presser)
        update()
    }

    override suspend fun released(presser: Presser) {
        currentlyPressing.remove(presser)
        update()
    }

    override suspend fun disconnected(presser: Presser) {
        logger.info("Presser disconnected")
        currentlyPressing.remove(presser)
        pressers.remove(presser)
        update()
    }

    suspend fun addPresser(presser: Presser) {
        pressers.add(presser)
        presser.updatePressingCount(currentlyPressing.count())
    }
}
