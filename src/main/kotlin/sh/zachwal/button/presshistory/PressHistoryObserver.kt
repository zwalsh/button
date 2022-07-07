package sh.zachwal.button.presshistory

import com.google.inject.Inject
import com.google.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import sh.zachwal.button.presser.Presser
import sh.zachwal.button.presser.PresserObserver
import java.util.concurrent.Executors

@Singleton
class PressHistoryObserver @Inject constructor(
    private val pressHistoryService: PressHistoryService
) : PresserObserver {

    private val scope = CoroutineScope(Executors.newFixedThreadPool(2).asCoroutineDispatcher())

    override suspend fun pressed(presser: Presser) {
        scope.launch {
            // Create the press in the background - don't block the caller
            pressHistoryService.createPress(presser.remoteHost)
        }
    }

    override suspend fun released(presser: Presser) {
    }

    override suspend fun disconnected(presser: Presser) {
    }
}
