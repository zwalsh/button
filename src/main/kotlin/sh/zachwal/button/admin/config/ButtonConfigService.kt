package sh.zachwal.button.admin.config

import org.slf4j.LoggerFactory
import sh.zachwal.button.admin.config.ButtonShape.SHAMROCK
import sh.zachwal.button.admin.config.ButtonShape.CIRCLE
import sh.zachwal.button.admin.config.ButtonShape.HEART
import sh.zachwal.button.config.AppConfig
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month.FEBRUARY
import java.time.Month.MARCH
import java.time.MonthDay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

private val valentinesDay = MonthDay.of(FEBRUARY, 14)
private val stPatricksDay = MonthDay.of(MARCH, 17)

class CurrentDateTime {
    fun now(): LocalDateTime = LocalDateTime.now()
}

@Singleton
class ButtonConfigService @Inject constructor(
    private val currentDateTime: CurrentDateTime = CurrentDateTime()
) {

    private val logger = LoggerFactory.getLogger(ButtonConfigService::class.java)

    private var override: ButtonShape? = null
    fun setOverride(shape: ButtonShape?) {
        override = shape
        logger.info("Set override to $shape")
    }

    fun currentShape(): ButtonShape {
        val date = currentDateTime.now().toLocalDate()

        if (withinDays(date, stPatricksDay, 3)) {
            return SHAMROCK
        }
        if (withinDays(date, valentinesDay, 3)) {
            return HEART
        }

        return CIRCLE
    }

    private fun withinDays(localDate: LocalDate, holiday: MonthDay, days: Int): Boolean {
        val dayOfYear = localDate.dayOfYear
        val holidayDayOfYear = holiday.atYear(localDate.year).dayOfYear
        return abs(holidayDayOfYear - dayOfYear) <= days
    }
}
