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
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread
import kotlin.math.max

data class DailyStatsSnapshot(val uniquePressers: Int, val peakConcurrent: Int, val totalPresses: Int)

sealed class DbOp
data class NewPress(val date: LocalDate) : DbOp()
data class NewPeak(val date: LocalDate, val newPeak: Int) : DbOp()
data class NewPresser(val date: LocalDate, val presserId: String) : DbOp()

@Singleton
class DailyStatsService @Inject constructor(
    private val dailyStatsDAO: DailyStatsDAO,
    private val dailyPressersDAO: DailyPressersDAO,
) : PresserObserver {

    private val logger = LoggerFactory.getLogger(DailyStatsService::class.java)

    internal var clock: Clock = Clock.systemDefaultZone()

    private val uniquePresserIds: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val currentlyPressing: MutableSet<Presser> = ConcurrentHashMap.newKeySet()
    private val peakConcurrent = AtomicInteger(0)
    private val totalPressCount = AtomicInteger(0)

    @Volatile
    private var trackingDate: LocalDate = LocalDate.now(clock)

    private val dbOpChannel = Channel<DbOp>(
        capacity = 1000,
        onBufferOverflow = BufferOverflow.DROP_LATEST,
    ) { undeliveredDbOp ->
        logger.error("Dropping a database write due to a buffer overflow: $undeliveredDbOp")
    }

    private val threadPool = Executors.newSingleThreadExecutor(
        ThreadFactoryBuilder().setNameFormat("daily-stats-db-%d").build()
    )
    private val scope = CoroutineScope(threadPool.asCoroutineDispatcher() + SupervisorJob())

    private val consumerJob: Job = scope.launch {
        for (op in dbOpChannel) {
            try {
                processDbOp(op)
            } catch (e: Exception) {
                logger.error("Failed to process DB op: $op", e)
            }
        }
    }

    init {
        Runtime.getRuntime().addShutdownHook(
            thread(start = false) { threadPool.shutdownNow() }
        )
        initialize()
    }

    fun initialize() {
        val today = LocalDate.now(clock)
        dailyStatsDAO.ensureRow(today)
        val row = dailyStatsDAO.findByDate(today)
        uniquePresserIds.clear()
        peakConcurrent.set(0)
        totalPressCount.set(0)
        if (row != null) {
            peakConcurrent.set(row.peakConcurrent)
            totalPressCount.set(row.totalPressCount)
        }
        uniquePresserIds.addAll(dailyPressersDAO.findByDate(today))
        trackingDate = today
    }

    override suspend fun pressed(presser: Presser) {
        val today = LocalDate.now(clock)
        if (today != trackingDate) {
            initialize()
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

    private fun updatePeak(concurrentCount: Int) {
        val prevPeak = peakConcurrent.getAndUpdate { max(it, concurrentCount) }
        if (concurrentCount > prevPeak) {
            dbOpChannel.trySend(NewPeak(trackingDate, concurrentCount))
        }
    }

    fun currentStats(): DailyStatsSnapshot = DailyStatsSnapshot(
        uniquePressers = uniquePresserIds.size,
        peakConcurrent = peakConcurrent.get(),
        totalPresses = totalPressCount.get(),
    )

    fun close() {
        dbOpChannel.close()
        runBlocking { consumerJob.join() } // drain remaining ops before stopping the thread pool
        threadPool.shutdown()
    }

    private fun processDbOp(op: DbOp) {
        when (op) {
            is NewPress -> dailyStatsDAO.incrementTotalPresses(op.date)
            is NewPeak -> dailyStatsDAO.updatePeakIfHigher(op.date, op.newPeak)
            is NewPresser -> dailyPressersDAO.insertIfAbsent(op.date, op.presserId)
        }
    }
}
