package sh.zachwal.button.presshistory

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.sqlobject.kotlin.onDemand
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import sh.zachwal.button.db.dao.DailyPressersDAO
import sh.zachwal.button.db.dao.DailyStatsDAO
import sh.zachwal.button.db.extension.DatabaseExtension
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.presser.Presser
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@ExtendWith(DatabaseExtension::class)
class DailyStatsServiceTest(private val jdbi: Jdbi) {

    private lateinit var dailyStatsDAO: DailyStatsDAO
    private lateinit var dailyPressersDAO: DailyPressersDAO
    private lateinit var service: DailyStatsService

    private val today = LocalDate.now()

    @BeforeEach
    fun setUp() {
        dailyStatsDAO = jdbi.onDemand()
        dailyPressersDAO = jdbi.onDemand()
        service = DailyStatsService(dailyStatsDAO, dailyPressersDAO)
        service.initialize()
    }

    @AfterEach
    fun tearDown() {
        // Drain pending DB ops before DatabaseExtension truncates tables
        service.close()
    }

    private fun mockPresser(remoteHost: String = "127.0.0.1", contact: Contact? = null): Presser {
        return mockk<Presser>().also {
            every { it.remoteHost } returns remoteHost
            every { it.contact } returns contact
        }
    }

    @Test
    fun `first press increments totalPresses and uniquePressers to 1`() = runBlocking {
        service.pressed(mockPresser("1.2.3.4"))

        val stats = service.currentStats()
        assertThat(stats.totalPresses).isEqualTo(1)
        assertThat(stats.uniquePressers).isEqualTo(1)
    }

    @Test
    fun `same presser pressing again increments totalPresses only`() = runBlocking {
        val presser = mockPresser("1.2.3.4")
        service.pressed(presser)
        service.pressed(presser)

        val stats = service.currentStats()
        assertThat(stats.totalPresses).isEqualTo(2)
        assertThat(stats.uniquePressers).isEqualTo(1)
    }

    @Test
    fun `new presser increments uniquePressers`() = runBlocking {
        service.pressed(mockPresser("1.2.3.4"))
        service.pressed(mockPresser("5.6.7.8"))

        val stats = service.currentStats()
        assertThat(stats.totalPresses).isEqualTo(2)
        assertThat(stats.uniquePressers).isEqualTo(2)
    }

    @Test
    fun `authenticated presser uses contact id as presser identity`() = runBlocking {
        val contact = Contact(id = 42, createdDate = Instant.now(), name = "Alice", phoneNumber = "", active = true)
        // authenticated presser from one host, anonymous from the same host — counted separately
        service.pressed(mockPresser("1.2.3.4", contact = contact))
        service.pressed(mockPresser("1.2.3.4", contact = null))

        val stats = service.currentStats()
        assertThat(stats.uniquePressers).isEqualTo(2)
    }

    @Test
    fun `updatePeak with higher count updates peak`() {
        service.updatePeak(5)

        assertThat(service.currentStats().peakConcurrent).isEqualTo(5)
    }

    @Test
    fun `updatePeak with lower count does not lower existing peak`() {
        service.updatePeak(5)
        service.updatePeak(3)

        assertThat(service.currentStats().peakConcurrent).isEqualTo(5)
    }

    @Test
    fun `concurrent pressed calls are thread-safe`() {
        val executor = Executors.newFixedThreadPool(4)
        val futures = (1..100).map { i ->
            executor.submit {
                runBlocking {
                    service.pressed(mockPresser("host-$i"))
                }
            }
        }
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)
        futures.forEach { it.get() }

        val stats = service.currentStats()
        assertThat(stats.totalPresses).isEqualTo(100)
        assertThat(stats.uniquePressers).isEqualTo(100)
    }

    @Test
    fun `initialize reloads state from DB after simulated restart`() {
        // Pre-populate DB directly (simulating prior service runs)
        dailyStatsDAO.ensureRow(today)
        dailyStatsDAO.incrementTotalPresses(today)
        dailyStatsDAO.incrementTotalPresses(today)
        dailyStatsDAO.incrementTotalPresses(today)
        dailyStatsDAO.updatePeakIfHigher(today, 4)
        dailyPressersDAO.insertIfAbsent(today, "presser-1")
        dailyPressersDAO.insertIfAbsent(today, "presser-2")

        // New service instance simulates a process restart
        val reloadedService = DailyStatsService(dailyStatsDAO, dailyPressersDAO)
        reloadedService.initialize()

        val stats = reloadedService.currentStats()
        assertThat(stats.totalPresses).isEqualTo(3)
        assertThat(stats.peakConcurrent).isEqualTo(4)
        assertThat(stats.uniquePressers).isEqualTo(2)
    }
}
