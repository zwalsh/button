package sh.zachwal.button.presshistory

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.inject.Inject
import com.google.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import sh.zachwal.button.presser.Presser
import sh.zachwal.button.presser.PresserObserver
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.concurrent.thread

@Singleton
class PressLogger @Inject constructor() : PresserObserver {
    private val logger = LoggerFactory.getLogger(PressLogger::class.java)
    private val pressCounts = ConcurrentHashMap<String, Int>()
    private val threadPool = Executors.newFixedThreadPool(
        2,
        ThreadFactoryBuilder()
            .setNameFormat("press-logger-%d")
            .build()
    )
    private val scope = CoroutineScope(threadPool.asCoroutineDispatcher() + SupervisorJob())
    private val pressChannel = Channel<String>(
        capacity = 100,
        onBufferOverflow = BufferOverflow.DROP_LATEST
    )

    init {
        scope.launch {
            while (true) {
                delay(10_000)
                try {
                    if (pressCounts.isNotEmpty()) {
                        pressCounts.forEach { (name, count) ->
                            logger.info("$name pressed $count times")
                        }
                        pressCounts.clear()
                    }
                } catch (e: Exception) {
                    logger.error("Failed to log presses", e)
                }
            }
        }

        scope.launch {
            for (key in pressChannel) {
                pressCounts.merge(key, 1, Int::plus)
            }
        }
        Runtime.getRuntime().addShutdownHook(
            thread(start = false) {
                threadPool.shutdownNow()
            }
        )
    }

    override suspend fun pressed(presser: Presser) {
        val key = presser.contact?.name ?: presser.remoteHost
        pressChannel.trySend(key)
    }

    override suspend fun released(presser: Presser) {}
    override suspend fun disconnected(presser: Presser) {}
}
