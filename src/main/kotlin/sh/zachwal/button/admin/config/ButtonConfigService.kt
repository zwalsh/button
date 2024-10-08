package sh.zachwal.button.admin.config

import org.slf4j.LoggerFactory
import sh.zachwal.button.admin.config.ButtonShape.ALPACA
import sh.zachwal.button.admin.config.ButtonShape.CHRISTMAS_TREE
import sh.zachwal.button.admin.config.ButtonShape.CIRCLE
import sh.zachwal.button.admin.config.ButtonShape.DEREK
import sh.zachwal.button.admin.config.ButtonShape.FIREWORKS
import sh.zachwal.button.admin.config.ButtonShape.HEART
import sh.zachwal.button.admin.config.ButtonShape.PUMPKIN
import sh.zachwal.button.admin.config.ButtonShape.RINGS
import sh.zachwal.button.admin.config.ButtonShape.SHAMROCK
import sh.zachwal.button.admin.config.ButtonShape.TURKEY
import sh.zachwal.button.admin.config.ButtonShape.WIGWAM
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month.DECEMBER
import java.time.Month.FEBRUARY
import java.time.Month.JULY
import java.time.Month.JUNE
import java.time.Month.MARCH
import java.time.Month.OCTOBER
import java.time.Month.SEPTEMBER
import java.time.MonthDay
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

private val valentinesDay = MonthDay.of(FEBRUARY, 14)
private val stPatricksDay = MonthDay.of(MARCH, 17)
private val fourthOfJuly = MonthDay.of(JULY, 4)
private val halloween = MonthDay.of(OCTOBER, 31)
private val christmas = MonthDay.of(DECEMBER, 25)
private val derekBachelorParty = listOf(
    LocalDate.of(2024, JUNE, 6),
    LocalDate.of(2024, JUNE, 7),
    LocalDate.of(2024, JUNE, 8),
    LocalDate.of(2024, JUNE, 9),
)
private val myWedding = LocalDate.of(2024, JUNE, 22)
private val derekAndLindsayWedding = LocalDate.of(2024, JULY, 6)
private val wigwam2024 = listOf(
    LocalDate.of(2024, SEPTEMBER, 27),
    LocalDate.of(2024, SEPTEMBER, 28),
    LocalDate.of(2024, SEPTEMBER, 29),
)

@Singleton
class ButtonConfigService @Inject constructor(
    private val currentDateTime: CurrentDateTime
) {

    private val logger = LoggerFactory.getLogger(ButtonConfigService::class.java)

    private var buttonShapeOverride: ButtonShape? = null
    fun setOverride(shape: ButtonShape?) {
        buttonShapeOverride = shape
        logger.info("Set override to $shape")
    }

    // This is in memory but should eventually probably go in the database
    fun getOverride(): ButtonShape? = buttonShapeOverride

    fun currentShape(): ButtonShape {
        buttonShapeOverride?.let {
            return it
        }

        val date = currentDateTime.now().toLocalDate()
        val thanksgivingDay = LocalDate.of(date.year, 11, 1)
            .with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY))

        return if (withinDays(date, stPatricksDay, 3)) {
            SHAMROCK
        } else if (withinDays(date, valentinesDay, 3)) {
            HEART
        } else if (date == derekAndLindsayWedding) {
            // check 7/6/24 before fourth of July
            ALPACA
        } else if (withinDays(date, fourthOfJuly, 2)) {
            FIREWORKS
        } else if (withinDays(date, christmas, 1)) {
            CHRISTMAS_TREE
        } else if (withinDays(date, thanksgivingDay, 1)) {
            TURKEY
        } else if (withinDays(date, halloween, 3)) {
            PUMPKIN
        } else if (date in derekBachelorParty) {
            DEREK
        } else if (date == myWedding) {
            RINGS
        } else if (date in wigwam2024) {
            WIGWAM
        } else {
            CIRCLE
        }
    }

    private fun withinDays(localDate: LocalDate, holiday: MonthDay, days: Int): Boolean =
        withinDays(localDate, holiday.atYear(localDate.year), days)

    private fun withinDays(localDate: LocalDate, holiday: LocalDate, days: Int): Boolean {
        val dayOfYear = localDate.dayOfYear
        val holidayDayOfYear = holiday.dayOfYear
        return abs(holidayDayOfYear - dayOfYear) <= days
    }
}
