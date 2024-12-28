package sh.zachwal.button.wrapped

import com.google.common.cache.CacheBuilder
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.inject.name.Named
import io.ktor.features.NotFoundException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.db.dao.WrappedDAO
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.db.jdbi.WrappedLink
import sh.zachwal.button.db.jdbi.WrappedRank
import sh.zachwal.button.random.RandomStringGenerator
import sh.zachwal.button.sms.ControlledContactMessagingService
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.format.TextStyle.FULL
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread
import kotlin.math.roundToInt

@Singleton
class WrappedService @Inject constructor(
    private val contactDAO: ContactDAO,
    private val wrappedDAO: WrappedDAO,
    private val controlledContactMessagingService: ControlledContactMessagingService,
    @Named("host")
    private val host: String,
) {
    private val threadPool = Executors.newFixedThreadPool(
        1,
        ThreadFactoryBuilder()
            .setNameFormat("wrapped-notifier-thread-%d")
            .build()
    )
    private val scope = CoroutineScope(threadPool.asCoroutineDispatcher() + SupervisorJob())
    private val link = "https://$host"

    private val easternTime = ZoneId.of("America/New_York")
    private val randomStringGenerator = RandomStringGenerator()
    private val logger = LoggerFactory.getLogger(WrappedService::class.java)

    private val wrappedRanksCache = CacheBuilder.newBuilder()
        .maximumSize(1)
        .expireAfterWrite(1, TimeUnit.DAYS)
        .build<Pair<Instant, Instant>, List<WrappedRank>>()

    private val wrappedCache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<String, Wrapped>()

    init {
        Runtime.getRuntime().addShutdownHook(
            thread(start = false) {
                threadPool.shutdownNow()
            }
        )
    }

    private fun startOfYearInstant(year: Int): Instant =
        LocalDate.of(year, Month.JANUARY, 1)
            .atStartOfDay(easternTime)
            .toInstant()

    private fun endOfYearInstant(year: Int): Instant =
        LocalDate.of(year + 1, Month.JANUARY, 1)
            .atStartOfDay(easternTime)
            .toInstant()

    fun createWrappedLinks() {
        val year = LocalDate.now().year
        val links = wrappedDAO.wrappedLinks()

        if (links.any { it.year == year }) {
            throw RuntimeException("Links have already been generated for $year!")
        }

        val contactIds = wrappedDAO.contactsWithPresses(
            fromInstant = startOfYearInstant(year),
            toInstant = endOfYearInstant(year)
        )

        contactIds.forEach { contactId ->
            logger.info("Generating link for $contactId")
            val wrappedId = randomStringGenerator.newToken(20)
            val wrappedLink = WrappedLink(
                wrappedId,
                year,
                contactId
            )
            logger.info("Generated link with id $wrappedId for year $year.")
            wrappedDAO.insertWrappedLink(wrappedLink)
        }
    }

    fun listWrappedLinks(): List<WrappedLink> {
        return wrappedDAO.wrappedLinks()
    }

    fun sendWrappedNotification() {
        val year = LocalDate.now().year
        val links = wrappedDAO.wrappedLinks().filter { it.year == year }
        logger.info("Sending ${links.size} wrapped notifications for year $year.")

        scope.launch {
            links.forEach { l ->
                val contact = contactDAO.findContact(l.contactId)!!
                val linkForContact = "$link/wrapped/$year/${l.wrappedId}"
                val message = "What a year, ${contact.name}!" +
                    " Check out your Button Wrapped, $year: $linkForContact"
                logger.info("Sending message to ${contact.id}: $message")
                controlledContactMessagingService.sendMessage(
                    contact = contact,
                    body = message
                )
                // Don't get our number blocked
                delay(1000)
            }
        }
    }

    private fun wrappedRanks(fromInstant: Instant, toInstant: Instant): List<WrappedRank> {
        return wrappedRanksCache.get(fromInstant to toInstant) {
            logger.info("Fetching wrapped ranks for $fromInstant to $toInstant.")
            wrappedDAO.wrappedRanks(fromInstant, toInstant)
        }
    }

    fun wrapped(year: Int, id: String): Wrapped {
        val wrappedLink = wrappedDAO.wrappedLinks()
            .find { it.wrappedId == id }
            ?: throw NotFoundException("Could not find Wrapped with id $id")

        val contact = contactDAO.findContact(wrappedLink.contactId) ?: throw NotFoundException(
            "Could not " +
                "find contact with id $id."
        )

        return wrappedCache.get(id) {
            buildWrapped(year, contact)
        }
    }

    private fun buildWrapped(year: Int, contact: Contact): Wrapped {
        logger.info("Building wrapped for $year and contact=$contact")
        val presses = wrappedDAO.selectBetweenForContact(
            begin = startOfYearInstant(year),
            end = endOfYearInstant(year),
            contactId = contact.id
        )
        val countByDay = presses.groupBy {
            LocalDate.ofInstant(it.time, easternTime).dayOfWeek
        }
        val favoriteDay = countByDay.entries.maxByOrNull {
            it.value.count()
        }!!

        val countByHour = presses.groupBy {
            LocalDateTime.ofInstant(it.time, easternTime).hour
        }
        val favoriteHour = countByHour.entries.maxByOrNull {
            it.value.count()
        }!!
        val hour = favoriteHour.key
        val favoriteHour12Hour = if (hour % 12 == 0) 12 else hour % 12
        val favoriteHourAmPm = if (favoriteHour.key < 12) {
            "AM"
        } else {
            "PM"
        }
        val favoriteHourString = "$favoriteHour12Hour$favoriteHourAmPm"

        val wrappedRanks = wrappedRanks(
            fromInstant = startOfYearInstant(year),
            toInstant = endOfYearInstant(year)
        )

        val wrappedRank = wrappedRanks.find { it.contactId == contact.id }!!

        return Wrapped(
            year = year,
            name = contact.name,
            count = presses.size,
            favoriteDay = favoriteDay.key.getDisplayName(FULL, Locale.US),
            favoriteDayCount = favoriteDay.value.size,
            favoriteHourString = favoriteHourString,
            favoriteHourCount = favoriteHour.value.size,
            rank = wrappedRank.rank,
            percentile = percentileAsInt(wrappedRank.percentile),
            uniqueDaysCount = wrappedRank.uniqueDays,
            uniqueDaysRank = wrappedRank.uniqueDaysRank,
            uniqueDaysPercentile = percentileAsInt(wrappedRank.uniqueDaysPercentile)
        )
    }

    private fun percentileAsInt(percentile: Double) =
        (percentile * 100)
            .roundToInt()
            .takeIf { it != 0 }
            ?: 1 // round 0% to 1%
}
