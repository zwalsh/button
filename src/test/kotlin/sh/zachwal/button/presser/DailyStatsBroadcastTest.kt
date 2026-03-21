package sh.zachwal.button.presser

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import sh.zachwal.button.presser.protocol.server.DailyStats
import sh.zachwal.button.presshistory.DailyStatsService
import sh.zachwal.button.presshistory.DailyStatsSnapshot

class DailyStatsBroadcastTest {

    private val stats = DailyStatsSnapshot(uniquePressers = 3, peakConcurrent = 2, totalPresses = 7)
    private val dailyStatsService = mockk<DailyStatsService>(relaxed = true).also {
        every { it.currentStats() } returns stats
    }
    private val expectedMessage = DailyStats(
        uniquePressers = stats.uniquePressers,
        peakConcurrent = stats.peakConcurrent,
        totalPresses = stats.totalPresses,
    )

    @Test
    fun `addPresser sends current DailyStats to new presser`() = runBlocking {
        val presser = mockk<Presser>(relaxed = true)
        every { presser.contact } returns null
        val manager = PresserManager(dailyStatsService)

        manager.addPresser(presser)

        coVerify { presser.sendDailyStats(expectedMessage) }
    }

    @Test
    fun `pressed broadcasts DailyStats to all pressers`() = runBlocking {
        val presser1 = mockk<Presser>(relaxed = true)
        val presser2 = mockk<Presser>(relaxed = true)
        every { presser1.contact } returns null
        every { presser2.contact } returns null
        val manager = PresserManager(dailyStatsService)
        manager.addPresser(presser1)
        manager.addPresser(presser2)

        manager.pressed(presser1)

        coVerify { presser1.sendDailyStats(expectedMessage) }
        coVerify { presser2.sendDailyStats(expectedMessage) }
    }

    @Test
    fun `pressed broadcasts updated DailyStats after stats change`() = runBlocking {
        val presser = mockk<Presser>(relaxed = true)
        every { presser.contact } returns null
        val manager = PresserManager(dailyStatsService)
        manager.addPresser(presser)

        val updatedStats = DailyStatsSnapshot(uniquePressers = 4, peakConcurrent = 3, totalPresses = 8)
        every { dailyStatsService.currentStats() } returns updatedStats

        manager.pressed(presser)

        coVerify {
            presser.sendDailyStats(
                DailyStats(
                    uniquePressers = updatedStats.uniquePressers,
                    peakConcurrent = updatedStats.peakConcurrent,
                    totalPresses = updatedStats.totalPresses,
                )
            )
        }
    }
}
