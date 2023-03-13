package sh.zachwal.button.admin.config

import org.slf4j.LoggerFactory
import sh.zachwal.button.admin.config.ButtonShape.SHAMROCK
import sh.zachwal.button.admin.config.ButtonShape.CIRCLE
import sh.zachwal.button.admin.config.ButtonShape.HEART
import sh.zachwal.button.config.AppConfig
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
        val dayOfYear = date.dayOfYear

        val stPatricksDayOfYear = stPatricksDay.atYear(date.year).dayOfYear
        if (abs(stPatricksDayOfYear - dayOfYear) <= 3) {
            return SHAMROCK
        }
        val valentinesDayOfYear = valentinesDay.atYear(date.year).dayOfYear
        if (abs(valentinesDayOfYear - dayOfYear) <= 3) {
            return HEART
        }

        return CIRCLE
    }
}
