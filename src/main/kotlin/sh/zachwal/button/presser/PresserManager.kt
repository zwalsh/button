package sh.zachwal.button.presser

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import sh.zachwal.button.presser.protocol.server.Snapshot
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.concurrent.thread

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

    private val threadPool = Executors.newSingleThreadExecutor(
        ThreadFactoryBuilder().setNameFormat("presser-manager-%d").build()
    )
    private val scope = CoroutineScope(threadPool.asCoroutineDispatcher() + SupervisorJob())

    private fun buildSnapshot(): Snapshot {
        val names = currentlyPressing.mapNotNull { it.contact?.name }
        return Snapshot(count = currentlyPressing.size, names = names)
    }

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
        presser.contact?.name?.let { name ->
            pressers.forEach { p ->
                p.notifyPersonReleased(name)
            }
        }
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
        presser.sendSnapshot(buildSnapshot())
    }
}
