package sh.zachwal.button.admin.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import sh.zachwal.button.admin.config.ButtonShape.SHAMROCK
import sh.zachwal.button.admin.config.ButtonShape.CIRCLE
import sh.zachwal.button.admin.config.ButtonShape.HEART
import sh.zachwal.button.config.AppConfig
import sh.zachwal.button.config.MessagingConfig
import sh.zachwal.button.config.TwilioConfig
import java.time.LocalDateTime

class ButtonConfigServiceTest {

    private val appConfig = AppConfig(
        "", "", "", "", TwilioConfig("", "", ""), MessagingConfig(0, ""), true
    )

    @Test
    fun `takes initial config value`() {
        val buttonConfigService = ButtonConfigService(appConfig)
        assertTrue(buttonConfigService.isCube())

        val buttonConfigServiceFalse = ButtonConfigService(appConfig.copy(cubeButton = false))
        assertFalse(buttonConfigServiceFalse.isCube())
    }

    @Test
    fun `updates when set`() {
        val buttonConfigService = ButtonConfigService(appConfig)
        buttonConfigService.setCube(false)

        assertFalse(buttonConfigService.isCube())

        buttonConfigService.setCube(true)
        assertTrue(buttonConfigService.isCube())
    }

    @Test
    fun `shape is HEART within 3 days of Valentine's`() {
        val dateTimes = (14 - 3..14 + 3).map { day ->
            LocalDateTime.of(2023, 2, day, 0, 1)
        }
        dateTimes.forEach { dateTime ->
            val buttonConfigService = ButtonConfigService(appConfig, currentDateTime = { dateTime })

            assertEquals(HEART, buttonConfigService.currentShape())
        }
    }
    @Test
    fun `shape is SHAMROCK within 3 days of St Paddy's`() {
        val dateTimes = (17 - 3..17 + 3).map { day ->
            LocalDateTime.of(2023, 3, day, 0, 1)
        }
        dateTimes.forEach { dateTime ->
            val buttonConfigService = ButtonConfigService(appConfig, currentDateTime = { dateTime })

            assertEquals(SHAMROCK, buttonConfigService.currentShape())
        }
    }

    @Test
    fun `shape is CIRCLE on normal day`() {
        val dateTime = LocalDateTime.of(2023, 3, 1, 0, 0)
        val buttonConfigService = ButtonConfigService(appConfig, currentDateTime = { dateTime })

        assertEquals(CIRCLE, buttonConfigService.currentShape())
    }
}
