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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// A Clock subclass whose date can be advanced in tests
private class AdjustableClock(
    @Volatile var date: LocalDate,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = AdjustableClock(date, zone)
    override fun instant(): Instant = date.atStartOfDay(zone).toInstant()
}

@ExtendWith(DatabaseExtension::class)
class DailyStatsServiceTest(private val jdbi: Jdbi) {

    private lateinit var dailyStatsDAO: DailyStatsDAO
    private lateinit var dailyPressersDAO: DailyPressersDAO
    private lateinit var service: DailyStatsService

    private val today = LocalDate.now()
    private val adjustableClock = AdjustableClock(today)

    @BeforeEach
    fun setUp() {
        dailyStatsDAO = jdbi.onDemand()
        dailyPressersDAO = jdbi.onDemand()
        // Long rollover interval so the timer doesn't fire spontaneously in non-timer tests
        service = DailyStatsService(
            dailyStatsDAO,
            dailyPressersDAO,
            DailyStatsConfig(adjustableClock, rolloverCheckIntervalMs = 60_000L),
        )
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
    fun `peak concurrent rises as more pressers press simultaneously`() = runBlocking {
        val presserA = mockPresser("1.1.1.1")
        val presserB = mockPresser("2.2.2.2")
        val presserC = mockPresser("3.3.3.3")

        service.pressed(presserA)
        assertThat(service.currentStats().peakConcurrent).isEqualTo(1)

        service.pressed(presserB)
        assertThat(service.currentStats().peakConcurrent).isEqualTo(2)

        service.released(presserA)
        assertThat(service.currentStats().peakConcurrent).isEqualTo(2) // peak doesn't drop on release

        service.pressed(presserC) // back to 2 concurrent, not a new peak
        assertThat(service.currentStats().peakConcurrent).isEqualTo(2)
    }

    @Test
    fun `peak concurrent is persisted to DB`() = runBlocking {
        val presserA = mockPresser("1.1.1.1")
        val presserB = mockPresser("2.2.2.2")

        service.pressed(presserA)
        service.pressed(presserB)
        service.released(presserA)

        service.close()

        val row = dailyStatsDAO.findByDate(today)
        assertThat(row!!.peakConcurrent).isEqualTo(2)
    }

    @Test
    fun `disconnected presser is no longer counted toward concurrent`() = runBlocking {
        val presserA = mockPresser("1.1.1.1")
        val presserB = mockPresser("2.2.2.2")
        val presserC = mockPresser("3.3.3.3")

        service.pressed(presserA)
        service.pressed(presserB)
        service.disconnected(presserA) // A disconnects without releasing

        service.pressed(presserC) // B + C = 2, not a new peak
        assertThat(service.currentStats().peakConcurrent).isEqualTo(2)
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
    fun `day rollover resets in-memory stats`() = runBlocking {
        val presserA = mockPresser("1.2.3.4")
        val presserB = mockPresser("5.6.7.8")
        service.pressed(presserA)
        service.pressed(presserB)
        service.released(presserA)
        service.released(presserB)
        assertThat(service.currentStats().totalPresses).isEqualTo(2)
        assertThat(service.currentStats().uniquePressers).isEqualTo(2)

        adjustableClock.date = today.plusDays(1)
        service.pressed(mockPresser("9.10.11.12"))

        val stats = service.currentStats()
        assertThat(stats.totalPresses).isEqualTo(1)
        assertThat(stats.uniquePressers).isEqualTo(1)
        assertThat(stats.peakConcurrent).isEqualTo(1)
    }

    @Test
    fun `presser still pressing at midnight is counted toward new day peak`() = runBlocking {
        val presserA = mockPresser("1.2.3.4")
        service.pressed(presserA) // pressing at end of day 1, never released

        adjustableClock.date = today.plusDays(1)
        service.pressed(mockPresser("5.6.7.8")) // triggers rollover; A is still pressing

        // A + new presser = 2 concurrent
        assertThat(service.currentStats().peakConcurrent).isEqualTo(2)
    }

    @Test
    fun `day rollover loads existing DB state for the new day`() = runBlocking {
        val tomorrow = today.plusDays(1)
        dailyStatsDAO.ensureRow(tomorrow)
        dailyStatsDAO.incrementTotalPresses(tomorrow)
        dailyStatsDAO.incrementTotalPresses(tomorrow)
        dailyStatsDAO.updatePeakIfHigher(tomorrow, 7)
        dailyPressersDAO.insertIfAbsent(tomorrow, "existing-presser")

        adjustableClock.date = tomorrow
        service.pressed(mockPresser("new-host"))

        val stats = service.currentStats()
        assertThat(stats.totalPresses).isEqualTo(3) // 2 pre-existing + 1 new press
        assertThat(stats.peakConcurrent).isEqualTo(7) // loaded from DB
        assertThat(stats.uniquePressers).isEqualTo(2) // "existing-presser" + "new-host"
    }

    @Test
    fun `day rollover does not trigger within the same day`() = runBlocking {
        service.pressed(mockPresser("1.2.3.4"))
        service.pressed(mockPresser("5.6.7.8"))
        service.pressed(mockPresser("1.2.3.4")) // same presser again

        val stats = service.currentStats()
        assertThat(stats.totalPresses).isEqualTo(3)
        assertThat(stats.uniquePressers).isEqualTo(2)
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
        val reloadedService = DailyStatsService(
            dailyStatsDAO,
            dailyPressersDAO,
            DailyStatsConfig(Clock.systemDefaultZone(), rolloverCheckIntervalMs = 60_000L),
        )
        reloadedService.initialize()

        val stats = reloadedService.currentStats()
        assertThat(stats.totalPresses).isEqualTo(3)
        assertThat(stats.peakConcurrent).isEqualTo(4)
        assertThat(stats.uniquePressers).isEqualTo(2)

        reloadedService.close()
    }

    // ---- Timer-triggered rollover tests ----

    private fun timerService(clock: AdjustableClock): DailyStatsService =
        DailyStatsService(
            dailyStatsDAO,
            dailyPressersDAO,
            DailyStatsConfig(clock, rolloverCheckIntervalMs = 50L),
        ).also { it.initialize() }

    @Test
    fun `timer fires at midnight and resets stats - press and release before midnight zeroes everything`() {
        service.close() // shut down the long-interval setUp service first

        val timerClock = AdjustableClock(today)
        val svc = timerService(timerClock)
        try {
            runBlocking {
                val presser = mockPresser("1.2.3.4")
                svc.pressed(presser)
                svc.released(presser)
            }
            assertThat(svc.currentStats().totalPresses).isEqualTo(1)

            timerClock.date = today.plusDays(1)
            Thread.sleep(200) // let the 50ms timer fire

            val stats = svc.currentStats()
            assertThat(stats.totalPresses).isEqualTo(0)
            assertThat(stats.uniquePressers).isEqualTo(0)
            assertThat(stats.peakConcurrent).isEqualTo(0)
        } finally {
            svc.close()
        }
    }

    @Test
    fun `timer fires at midnight and current pressers carry over as unique pressers and peak`() {
        service.close() // shut down the long-interval setUp service first

        val timerClock = AdjustableClock(today)
        val svc = timerService(timerClock)
        try {
            val presserA = mockPresser("1.2.3.4")
            runBlocking { svc.pressed(presserA) } // press but never release

            timerClock.date = today.plusDays(1)
            Thread.sleep(200) // let the 50ms timer fire

            val stats = svc.currentStats()
            assertThat(stats.totalPresses).isEqualTo(0)
            assertThat(stats.uniquePressers).isEqualTo(1)
            assertThat(stats.peakConcurrent).isEqualTo(1)
        } finally {
            svc.close()
        }
    }

    @Test
    fun `timer rollover writes currently-pressing users to DB as new day pressers`() {
        service.close()

        val timerClock = AdjustableClock(today)
        val svc = timerService(timerClock)
        try {
            val presserA = mockPresser("1.2.3.4")
            runBlocking { svc.pressed(presserA) } // never released

            timerClock.date = today.plusDays(1)
            Thread.sleep(200)

            svc.close() // drain DB ops

            val tomorrow = today.plusDays(1)
            val pressers = dailyPressersDAO.findByDate(tomorrow)
            assertThat(pressers).contains("1.2.3.4")
        } finally {
            // svc already closed above; suppress double-close
        }
    }
}
