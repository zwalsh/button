package sh.zachwal.button.presser

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import sh.zachwal.button.db.jdbi.Contact
import java.time.Instant

class PersonPressingBroadcastTest {
    @Test
    fun `broadcasts PersonPressing to all other pressers when a new person presses`() = runBlocking {
        val now = Instant.parse("2025-12-06T16:45:42.742Z")
        val contact1 = Contact(1, now, "Alice", "alice@example.com", true)
        val contact2 = Contact(2, now, "Bob", "bob@example.com", true)
        val presser1 = mockk<Presser>(relaxed = true)
        val presser2 = mockk<Presser>(relaxed = true)
        every { presser1.contact } returns contact1
        every { presser2.contact } returns contact2
        val manager = PresserManager()
        manager.addPresser(presser1)
        manager.addPresser(presser2)
        // Simulate presser1 pressing
        manager.pressed(presser1)
        // Both pressers should be notified about Alice
        coVerify { presser2.notifyPersonPressing("Alice") }
        coVerify { presser1.notifyPersonPressing("Alice") }
    }

    @Test
    fun `broadcasts PersonReleased to all pressers when a person releases`() = runBlocking {
        val now = Instant.parse("2025-12-06T16:45:42.742Z")
        val contact1 = Contact(1, now, "Alice", "alice@example.com", true)
        val contact2 = Contact(2, now, "Bob", "bob@example.com", true)
        val presser1 = mockk<Presser>(relaxed = true)
        val presser2 = mockk<Presser>(relaxed = true)
        every { presser1.contact } returns contact1
        every { presser2.contact } returns contact2
        val manager = PresserManager()
        manager.addPresser(presser1)
        manager.addPresser(presser2)
        // Simulate presser1 releasing
        manager.released(presser1)
        // Both pressers should be notified about Alice's release
        coVerify { presser2.notifyPersonReleased("Alice") }
        coVerify { presser1.notifyPersonReleased("Alice") }
    }
}
