package sh.zachwal.button.admin.config

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import sh.zachwal.button.admin.config.ButtonShape.CIRCLE
import sh.zachwal.button.admin.config.ButtonShape.CUBE
import sh.zachwal.button.admin.config.ButtonShape.HEART
import sh.zachwal.button.admin.config.ButtonShape.SHAMROCK
import java.time.LocalDateTime

class ButtonConfigServiceTest {

    private val dateTime = LocalDateTime.of(2023, 3, 1, 0, 0)
    private val currentDateTime = mockk<CurrentDateTime> { every { now() } returns dateTime }

    @Test
    fun `shape is CIRCLE on normal day`() {
        val buttonConfigService = ButtonConfigService(currentDateTime = currentDateTime)

        assertEquals(CIRCLE, buttonConfigService.currentShape())
    }

    @Test
    fun `uses override when set`() {
        val buttonConfigService = ButtonConfigService(currentDateTime)

        buttonConfigService.setOverride(CUBE)

        assertEquals(CUBE, buttonConfigService.currentShape())
    }

    @Test
    fun `can clear override`() {
        val buttonConfigService = ButtonConfigService(currentDateTime)

        buttonConfigService.setOverride(CUBE)
        buttonConfigService.setOverride(null)

        assertEquals(CIRCLE, buttonConfigService.currentShape())
    }

    @Test
    fun `shape is HEART within 3 days of Valentine's`() {
        val dateTimes = (14 - 3..14 + 3).map { day ->
            LocalDateTime.of(2023, 2, day, 0, 1)
        }
        dateTimes.forEach { dateTime ->
            val currentDateTime = mockk<CurrentDateTime> { every { now() } returns dateTime }
            val buttonConfigService = ButtonConfigService(currentDateTime = currentDateTime)

            assertEquals(HEART, buttonConfigService.currentShape())
        }
    }

    @Test
    fun `shape is SHAMROCK within 3 days of St Paddy's`() {
        val dateTimes = (17 - 3..17 + 3).map { day ->
            LocalDateTime.of(2023, 3, day, 0, 1)
        }
        dateTimes.forEach { dateTime ->
            val currentDateTime = mockk<CurrentDateTime> { every { now() } returns dateTime }
            val buttonConfigService = ButtonConfigService(currentDateTime = currentDateTime)

            assertEquals(SHAMROCK, buttonConfigService.currentShape())
        }
    }

    @Test
    fun `uses override on holiday`() {
        val stPatricksDay = LocalDateTime.of(2023, 3, 17, 0, 0)
        val currentDateTime = mockk<CurrentDateTime> { every { now() } returns stPatricksDay }
        val buttonConfigService = ButtonConfigService(currentDateTime = currentDateTime)
        assertEquals(SHAMROCK, buttonConfigService.currentShape())

        buttonConfigService.setOverride(CIRCLE)
        assertEquals(CIRCLE, buttonConfigService.currentShape())
    }
}
