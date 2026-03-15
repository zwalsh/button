package sh.zachwal.button.presser

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.presser.protocol.server.Snapshot
import java.time.Instant

class SnapshotTest {

    private val now = Instant.parse("2025-12-06T16:45:42.742Z")

    @Test
    fun `addPresser sends snapshot to new presser with empty state`() = runBlocking {
        val presser = mockk<Presser>(relaxed = true)
        every { presser.contact } returns null
        val manager = PresserManager()

        manager.addPresser(presser)

        coVerify { presser.sendSnapshot(Snapshot(count = 0, names = emptyList())) }
    }

    @Test
    fun `addPresser sends snapshot with current pressing count and authenticated names`() = runBlocking {
        val contact1 = Contact(1, now, "Alice", "alice@example.com", true)
        val presser1 = mockk<Presser>(relaxed = true)
        every { presser1.contact } returns contact1
        val anonymousPresser = mockk<Presser>(relaxed = true)
        every { anonymousPresser.contact } returns null
        val newPresser = mockk<Presser>(relaxed = true)
        every { newPresser.contact } returns null

        val manager = PresserManager()
        manager.addPresser(presser1)
        manager.addPresser(anonymousPresser)
        manager.pressed(presser1)
        manager.pressed(anonymousPresser)

        manager.addPresser(newPresser)

        coVerify { newPresser.sendSnapshot(Snapshot(count = 2, names = listOf("Alice"))) }
    }

    @Test
    fun `snapshot only includes authenticated presser names, not anonymous`() = runBlocking {
        val contact = Contact(1, now, "Alice", "alice@example.com", true)
        val authenticatedPresser = mockk<Presser>(relaxed = true)
        every { authenticatedPresser.contact } returns contact
        val anonymousPresser = mockk<Presser>(relaxed = true)
        every { anonymousPresser.contact } returns null
        val newPresser = mockk<Presser>(relaxed = true)
        every { newPresser.contact } returns null

        val manager = PresserManager()
        manager.addPresser(authenticatedPresser)
        manager.addPresser(anonymousPresser)
        manager.pressed(authenticatedPresser)
        manager.pressed(anonymousPresser)

        manager.addPresser(newPresser)

        // count is 2 (both pressing), but names only contains authenticated user
        coVerify { newPresser.sendSnapshot(Snapshot(count = 2, names = listOf("Alice"))) }
    }
}
