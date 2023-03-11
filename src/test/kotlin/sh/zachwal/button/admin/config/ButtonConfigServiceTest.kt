package sh.zachwal.button.admin.config

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import sh.zachwal.button.config.AppConfig
import sh.zachwal.button.config.MessagingConfig
import sh.zachwal.button.config.TwilioConfig

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
}
