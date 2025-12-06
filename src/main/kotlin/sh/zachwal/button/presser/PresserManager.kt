package sh.zachwal.button.presser

import com.google.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Orchestrates communications between [Presser] instances.
 *
 * Holds a list of all active Presser instances. This class is notified via [PresserObserver] when a Presser's state
 * changes, and it broadcasts relevant updates to all other Presser instances.
 */
@Singleton
class PresserManager : PresserObserver {

    private val logger = LoggerFactory.getLogger(PresserManager::class.java)

    private val pressers: MutableSet<Presser> = ConcurrentHashMap.newKeySet()
    private val currentlyPressing: MutableSet<Presser> = ConcurrentHashMap.newKeySet()

    private suspend fun sendCurrentCount() {
        val pressingCount = currentlyPressing.count()
        pressers.forEach { presser ->
            presser.updatePressingCount(pressingCount)
        }
    }

    private suspend fun sendNewPresser(presser: Presser) {
        presser.contact?.name?.let { name ->
            pressers
                .forEach { p ->
                    p.notifyPersonPressing(name)
                }
        }
    }

    override suspend fun pressed(presser: Presser) {
        currentlyPressing.add(presser)
        sendCurrentCount()
        sendNewPresser(presser)
    }

    override suspend fun released(presser: Presser) {
        currentlyPressing.remove(presser)
        sendCurrentCount()
    }

    override suspend fun disconnected(presser: Presser) {
        logger.info("Presser disconnected")
        currentlyPressing.remove(presser)
        pressers.remove(presser)
        sendCurrentCount()
    }

    suspend fun addPresser(presser: Presser) {
        pressers.add(presser)
        presser.updatePressingCount(currentlyPressing.count())
    }
}
