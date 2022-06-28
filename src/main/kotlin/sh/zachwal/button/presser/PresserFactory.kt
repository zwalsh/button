package sh.zachwal.button.presser

import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import io.ktor.websocket.WebSocketServerSession
import kotlinx.coroutines.CoroutineDispatcher
import sh.zachwal.button.presshistory.PressHistoryObserver

@Singleton
class PresserFactory @Inject constructor(
    private val presserManager: PresserManager,
    private val presserHistoryObserver: PressHistoryObserver,
    @Named("presserDispatcher")
    private val presserDispatcher: CoroutineDispatcher,
) {

    fun createPresser(socketSession: WebSocketServerSession, remoteHost: String): Presser {
        val observer = MultiPresserObserver(
            listOf(
                presserManager, presserHistoryObserver
            )
        )
        return Presser(socketSession, observer, remoteHost, presserDispatcher)
    }
}
