package sh.zachwal.button.presser

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.truth.Truth.assertThat
import io.ktor.server.websocket.WebSocketServerSession
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Test
import sh.zachwal.button.db.jdbi.contact
import sh.zachwal.button.notify.ContactNotifier
import sh.zachwal.button.presshistory.PressHistoryObserver
import sh.zachwal.button.presshistory.PressLogger

class PresserFactoryTest {

    private val animalEmojiAssigner = mockk<AnimalEmojiAssigner>()
    private val factory = PresserFactory(
        presserManager = mockk(relaxed = true),
        dailyStatsService = mockk(relaxed = true),
        presserHistoryObserver = mockk<PressHistoryObserver>(relaxed = true),
        contactNotifier = mockk<ContactNotifier>(relaxed = true),
        pressLogger = mockk<PressLogger>(relaxed = true),
        presserDispatcher = Dispatchers.IO,
        mapper = mockk<ObjectMapper>(relaxed = true),
        animalEmojiAssigner = animalEmojiAssigner,
    )

    @Test
    fun `createPresser uses the contact's name when a contact is present`() {
        val contact = contact(name = "Alice")

        val presser = factory.createPresser(mockk<WebSocketServerSession>(relaxed = true), "host", contact)

        assertThat(presser.name()).isEqualTo("Alice")
    }

    @Test
    fun `createPresser assigns an animal emoji from the assigner when no contact is present`() {
        every { animalEmojiAssigner.next() } returns "🐱"

        val presser = factory.createPresser(mockk<WebSocketServerSession>(relaxed = true), "host", null)

        assertThat(presser.name()).isEqualTo("🐱")
    }
}
