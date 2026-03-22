package sh.zachwal.button.presshistory

import com.google.common.util.concurrent.ThreadFactoryBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import sh.zachwal.button.db.dao.DailyPressersDAO
import sh.zachwal.button.db.dao.DailyStatsDAO
import sh.zachwal.button.presser.Presser
import sh.zachwal.button.presser.PresserObserver
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread
import kotlin.math.max

data class DailyStatsSnapshot(val uniquePressers: Int, val peakConcurrent: Int, val totalPresses: Int)

data class DailyStatsConfig(
    val clock: Clock,
    val rolloverCheckIntervalMs: Long,
)

sealed class DbOp
data class NewPress(val date: LocalDate) : DbOp()
data class NewPeak(val date: LocalDate, val newPeak: Int) : DbOp()
data class NewPresser(val date: LocalDate, val presserId: String) : DbOp()

@Singleton
class DailyStatsService @Inject constructor(
    private val dailyStatsDAO: DailyStatsDAO,
    private val dailyPressersDAO: DailyPressersDAO,
    private val config: DailyStatsConfig,
) : PresserObserver {

    private val logger = LoggerFactory.getLogger(DailyStatsService::class.java)
    private val clock: Clock get() = config.clock

    // --- In-memory state ---

    private val uniquePresserIds: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val currentlyPressing: MutableSet<Presser> = ConcurrentHashMap.newKeySet()
    private val peakConcurrent = AtomicInteger(0)
    private val totalPressCount = AtomicInteger(0)

    @Volatile
    private var trackingDate: LocalDate = LocalDate.now(clock)

    // --- Async DB write infrastructure ---

    private val dbOpChannel = Channel<DbOp>(
        capacity = 1000,
        onBufferOverflow = BufferOverflow.DROP_LATEST,
    ) { undeliveredDbOp ->
        logger.error("Dropping a database write due to a buffer overflow: $undeliveredDbOp")
    }

    // Single-threaded pool for DB writes (consumer of dbOpChannel)
    private val dbThreadPool = Executors.newSingleThreadExecutor(
        ThreadFactoryBuilder().setNameFormat("daily-stats-db-%d").build()
    )
    private val scope = CoroutineScope(dbThreadPool.asCoroutineDispatcher() + SupervisorJob())
    private val consumerJob: Job = scope.launch {
        for (op in dbOpChannel) {
            try {
                processDbOp(op)
            } catch (e: Exception) {
                logger.error("Failed to process DB op: $op", e)
            }
        }
    }

    // Separate pool for the midnight rollover timer — must not share threads with the DB
    // pool so a slow DB write never delays the rollover check.
    private val rolloverThreadPool = Executors.newSingleThreadScheduledExecutor(
        ThreadFactoryBuilder().setNameFormat("daily-stats-rollover-%d").build()
    )

    init {
        Runtime.getRuntime().addShutdownHook(
            thread(start = false) {
                rolloverThreadPool.shutdownNow()
                dbThreadPool.shutdownNow()
            }
        )
        initialize()
        rolloverThreadPool.scheduleAtFixedRate(
            ::checkRollover,
            config.rolloverCheckIntervalMs,
            config.rolloverCheckIntervalMs,
            TimeUnit.MILLISECONDS,
        )
    }

    // --- Lifecycle ---

    /**
     * Loads stats for the current day from the DB. Called on fresh boot.
     * Does not account for any users who may currently be pressing.
     */
    fun initialize() {
        loadNewDay(LocalDate.now(clock))
    }

    fun close() {
        rolloverThreadPool.shutdown()
        rolloverThreadPool.awaitTermination(5, TimeUnit.SECONDS)
        dbOpChannel.close()
        runBlocking { consumerJob.join() } // drain remaining ops before stopping the thread pool
        dbThreadPool.shutdown()
    }

    // --- Public API ---

    override suspend fun pressed(presser: Presser) {
        val today = LocalDate.now(clock)
        if (today != trackingDate) {
            rollover()
        }
        currentlyPressing.add(presser)
        updatePeak(currentlyPressing.size)
        val presserId = presser.contact?.id?.toString() ?: presser.remoteHost
        totalPressCount.incrementAndGet()
        val isNewPresser = uniquePresserIds.add(presserId)
        if (isNewPresser) {
            dbOpChannel.trySend(NewPresser(today, presserId))
        }
        dbOpChannel.trySend(NewPress(today))
    }

    override suspend fun released(presser: Presser) {
        currentlyPressing.remove(presser)
    }

    override suspend fun disconnected(presser: Presser) {
        currentlyPressing.remove(presser)
    }

    fun currentStats(): DailyStatsSnapshot = DailyStatsSnapshot(
        uniquePressers = uniquePresserIds.size,
        peakConcurrent = peakConcurrent.get(),
        totalPresses = totalPressCount.get(),
    )

    // --- Rollover implementation ---

    private fun checkRollover() {
        if (LocalDate.now(clock) != trackingDate) {
            rollover()
        }
    }

    /**
     * Rolls over to a new day. Resets in-memory stats, reloads from DB, and carries
     * currently-pressing users into the new day as unique pressers and initial peak.
     * Called both by the periodic timer and by [pressed] when a press detects a date change.
     */
    internal fun rollover() {
        val newDay = LocalDate.now(clock)
        val pressersAtRollover = currentlyPressing.toSet()
        loadNewDay(newDay)
        for (presser in pressersAtRollover) {
            val presserId = presser.contact?.id?.toString() ?: presser.remoteHost
            val isNew = uniquePresserIds.add(presserId)
            if (isNew) {
                dbOpChannel.trySend(NewPresser(newDay, presserId))
            }
        }
        if (pressersAtRollover.isNotEmpty()) {
            updatePeak(pressersAtRollover.size)
        }
    }

    private fun loadNewDay(date: LocalDate) {
        dailyStatsDAO.ensureRow(date)
        val row = dailyStatsDAO.findByDate(date)
        uniquePresserIds.clear()
        peakConcurrent.set(0)
        totalPressCount.set(0)
        if (row != null) {
            peakConcurrent.set(row.peakConcurrent)
            totalPressCount.set(row.totalPressCount)
        }
        uniquePresserIds.addAll(dailyPressersDAO.findByDate(date))
        trackingDate = date
    }

    // --- DB write implementation ---

    private fun updatePeak(concurrentCount: Int) {
        val prevPeak = peakConcurrent.getAndUpdate { max(it, concurrentCount) }
        if (concurrentCount > prevPeak) {
            dbOpChannel.trySend(NewPeak(trackingDate, concurrentCount))
        }
    }

    private fun processDbOp(op: DbOp) {
        when (op) {
            is NewPress -> dailyStatsDAO.incrementTotalPresses(op.date)
            is NewPeak -> dailyStatsDAO.updatePeakIfHigher(op.date, op.newPeak)
            is NewPresser -> dailyPressersDAO.insertIfAbsent(op.date, op.presserId)
        }
    }
}
