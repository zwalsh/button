package sh.zachwal.button.presser

import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import sh.zachwal.button.db.jdbi.contact
import sh.zachwal.button.presser.protocol.server.DailyStats
import sh.zachwal.button.presser.protocol.server.Snapshot
import sh.zachwal.button.presshistory.DailyStatsService
import sh.zachwal.button.presshistory.DailyStatsSnapshot
class SnapshotTest {

    private val emptyDailyStats = DailyStatsSnapshot(0, 0, 0)
    private val dailyStatsService = mockk<DailyStatsService>(relaxed = true).also {
        every { it.currentStats() } returns emptyDailyStats
    }

    private fun snapshotWithDailyStats(count: Int, names: List<String>) = Snapshot(
        count = count,
        names = names,
        dailyStats = DailyStats(
            uniquePressers = emptyDailyStats.uniquePressers,
            peakConcurrent = emptyDailyStats.peakConcurrent,
            totalPresses = emptyDailyStats.totalPresses,
        ),
    )

    @Test
    fun `addPresser sends snapshot to new presser with empty state`() = runBlocking {
        val presser = mockk<Presser>(relaxed = true)
        every { presser.contact } returns null
        val manager = PresserManager(dailyStatsService)

        manager.addPresser(presser)

        coVerify { presser.sendSnapshot(snapshotWithDailyStats(count = 0, names = emptyList())) }
    }

    @Test
    fun `addPresser sends snapshot with current pressing count and authenticated names`() = runBlocking {
        val contact1 = contact(id = 1, name = "Alice")
        val presser1 = mockk<Presser>(relaxed = true)
        every { presser1.contact } returns contact1
        every { presser1.name() } returns contact1.name
        val anonymousPresser = mockk<Presser>(relaxed = true)
        every { anonymousPresser.contact } returns null
        every { anonymousPresser.name() } returns "🐱"
        val newPresser = mockk<Presser>(relaxed = true)
        every { newPresser.contact } returns null

        val manager = PresserManager(dailyStatsService)
        manager.addPresser(presser1)
        manager.addPresser(anonymousPresser)
        manager.pressed(presser1)
        manager.pressed(anonymousPresser)

        manager.addPresser(newPresser)

        val sent = slot<Snapshot>()
        coVerify { newPresser.sendSnapshot(capture(sent)) }
        assertThat(sent.captured.count).isEqualTo(2)
        assertThat(sent.captured.names).containsExactly("Alice", "🐱")
    }

    @Test
    fun `snapshot includes anonymous presser names as their assigned animal emoji`() = runBlocking {
        val contact = contact(id = 1, name = "Alice")
        val authenticatedPresser = mockk<Presser>(relaxed = true)
        every { authenticatedPresser.contact } returns contact
        every { authenticatedPresser.name() } returns contact.name
        val anonymousPresser = mockk<Presser>(relaxed = true)
        every { anonymousPresser.contact } returns null
        every { anonymousPresser.name() } returns "🐶"
        val newPresser = mockk<Presser>(relaxed = true)
        every { newPresser.contact } returns null

        val manager = PresserManager(dailyStatsService)
        manager.addPresser(authenticatedPresser)
        manager.addPresser(anonymousPresser)
        manager.pressed(authenticatedPresser)
        manager.pressed(anonymousPresser)

        manager.addPresser(newPresser)

        // count is 2 (both pressing), names contains the authenticated user and the anonymous presser's emoji
        val sent = slot<Snapshot>()
        coVerify { newPresser.sendSnapshot(capture(sent)) }
        assertThat(sent.captured.count).isEqualTo(2)
        assertThat(sent.captured.names).containsExactly("Alice", "🐶")
    }
}
