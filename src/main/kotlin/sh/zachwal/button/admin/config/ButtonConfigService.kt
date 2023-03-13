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

@Singleton
class ButtonConfigService @Inject constructor(
    config: AppConfig,
    private val currentDateTime: () -> LocalDateTime = { LocalDateTime.now() }
) {

    private val logger = LoggerFactory.getLogger(ButtonConfigService::class.java)

    private var isCube = config.cubeButton

    fun isCube(): Boolean = isCube

    fun setCube(cube: Boolean) {
        isCube = cube
        logger.info("Set cube to $isCube")
    }

    fun currentShape(): ButtonShape {
        val date = currentDateTime().toLocalDate()

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
