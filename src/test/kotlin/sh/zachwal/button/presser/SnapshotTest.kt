package sh.zachwal.button.presser

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
        val anonymousPresser = mockk<Presser>(relaxed = true)
        every { anonymousPresser.contact } returns null
        val newPresser = mockk<Presser>(relaxed = true)
        every { newPresser.contact } returns null

        val manager = PresserManager(dailyStatsService)
        manager.addPresser(presser1)
        manager.addPresser(anonymousPresser)
        manager.pressed(presser1)
        manager.pressed(anonymousPresser)

        manager.addPresser(newPresser)

        coVerify { newPresser.sendSnapshot(snapshotWithDailyStats(count = 2, names = listOf("Alice"))) }
    }

    @Test
    fun `snapshot only includes authenticated presser names, not anonymous`() = runBlocking {
        val contact = contact(id = 1, name = "Alice")
        val authenticatedPresser = mockk<Presser>(relaxed = true)
        every { authenticatedPresser.contact } returns contact
        val anonymousPresser = mockk<Presser>(relaxed = true)
        every { anonymousPresser.contact } returns null
        val newPresser = mockk<Presser>(relaxed = true)
        every { newPresser.contact } returns null

        val manager = PresserManager(dailyStatsService)
        manager.addPresser(authenticatedPresser)
        manager.addPresser(anonymousPresser)
        manager.pressed(authenticatedPresser)
        manager.pressed(anonymousPresser)

        manager.addPresser(newPresser)

        // count is 2 (both pressing), but names only contains authenticated user
        coVerify { newPresser.sendSnapshot(snapshotWithDailyStats(count = 2, names = listOf("Alice"))) }
    }
}
