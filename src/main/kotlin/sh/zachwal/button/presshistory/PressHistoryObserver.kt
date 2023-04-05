package sh.zachwal.button.presshistory

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.inject.Inject
import com.google.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import sh.zachwal.button.db.dao.PressDAO
import sh.zachwal.button.presser.Presser
import sh.zachwal.button.presser.PresserObserver
import java.util.concurrent.Executors
import kotlin.concurrent.thread

@Singleton
class PressHistoryObserver @Inject constructor(
    private val pressDAO: PressDAO
) : PresserObserver {

    private val threadPool = Executors.newFixedThreadPool(
        2,
        ThreadFactoryBuilder()
            .setNameFormat("press-history-thread-%d")
            .build()
    )

    init {
        Runtime.getRuntime().addShutdownHook(
            thread(start = false) {
                threadPool.shutdownNow()
            }
        )
    }

    // run with supervisor to prevent one failure from blocking future attempts
    private val scope = CoroutineScope(threadPool.asCoroutineDispatcher() + SupervisorJob())

    override suspend fun pressed(presser: Presser) {
        scope.launch {
            // Create the press in the background - don't block the caller
            pressDAO.createPress(presser.remoteHost)
        }
    }

    override suspend fun released(presser: Presser) {
    }

    override suspend fun disconnected(presser: Presser) {
    }
}
