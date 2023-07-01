package sh.zachwal.button.admin.config

import org.slf4j.LoggerFactory
import sh.zachwal.button.admin.config.ButtonShape.CHRISTMAS_TREE
import sh.zachwal.button.admin.config.ButtonShape.CIRCLE
import sh.zachwal.button.admin.config.ButtonShape.FIREWORKS
import sh.zachwal.button.admin.config.ButtonShape.HEART
import sh.zachwal.button.admin.config.ButtonShape.SHAMROCK
import sh.zachwal.button.admin.config.ButtonShape.TURKEY
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month.DECEMBER
import java.time.Month.FEBRUARY
import java.time.Month.JULY
import java.time.Month.MARCH
import java.time.MonthDay
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

private val valentinesDay = MonthDay.of(FEBRUARY, 14)
private val stPatricksDay = MonthDay.of(MARCH, 17)
private val fourthOfJuly = MonthDay.of(JULY, 4)
private val christmas = MonthDay.of(DECEMBER, 25)

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
        } else if (withinDays(date, fourthOfJuly, 1)) {
            FIREWORKS
        } else if (withinDays(date, christmas, 1)) {
            CHRISTMAS_TREE
        } else if (withinDays(date, thanksgivingDay, 1)) {
            TURKEY
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
