package sh.zachwal.button.presser

import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import io.ktor.websocket.WebSocketServerSession
import kotlinx.coroutines.CoroutineDispatcher
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.notify.ContactNotifier
import sh.zachwal.button.presshistory.PressHistoryObserver

@Singleton
class PresserFactory @Inject constructor(
    private val presserManager: PresserManager,
    private val presserHistoryObserver: PressHistoryObserver,
    private val contactNotifier: ContactNotifier,
    @Named("presserDispatcher")
    private val presserDispatcher: CoroutineDispatcher,
) {

    fun createPresser(
        socketSession: WebSocketServerSession,
        remoteHost: String,
        contact: Contact?
    ): Presser {
        val observer = MultiPresserObserver(
            listOf(
                presserManager, presserHistoryObserver, contactNotifier
            )
        )
        return Presser(socketSession, observer, remoteHost, contact, presserDispatcher)
    }
}
