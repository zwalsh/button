package sh.zachwal.button.admin.contact

import org.junit.jupiter.api.Test
import sh.zachwal.button.db.jdbi.NotificationPreferences
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.test.assertEquals

internal class AdminContactControllerTest {

    /**
     * Mirrors the private formatting logic in `formatSnoozedUntil` (year-aware "MMM d" /
     * "MMM d, yyyy" in America/New_York) so expectations track whatever "now" is at test
     * time, instead of a hardcoded date that eventually lands in the past.
     */
    private fun expectedSnoozeLabel(instant: Instant): String {
        val zone = ZoneId.of("America/New_York")
        val zoned = instant.atZone(zone)
        val pattern = if (zoned.year == Instant.now().atZone(zone).year) "MMM d" else "MMM d, yyyy"
        return DateTimeFormatter.ofPattern(pattern, Locale.US).format(zoned)
    }

    @Test
    fun `notificationLines shows Notifications on by default`() {
        val prefs = NotificationPreferences(
            notificationsEnabled = true,
            snoozedUntil = null,
            quietHoursStart = null,
            quietHoursEnd = null,
            timezone = null,
        )
        assertEquals(listOf("Notifications on"), notificationLines(prefs))
    }

    @Test
    fun `notificationLines shows Notifications off when disabled`() {
        val prefs = NotificationPreferences(
            notificationsEnabled = false,
            snoozedUntil = null,
            quietHoursStart = null,
            quietHoursEnd = null,
            timezone = null,
        )
        assertEquals(listOf("Notifications off"), notificationLines(prefs))
    }

    @Test
    fun `notificationLines shows snooze when snoozedUntil is in the future`() {
        val snoozedUntil = Instant.now().plus(30, ChronoUnit.DAYS)
        val prefs = NotificationPreferences(
            notificationsEnabled = true,
            snoozedUntil = snoozedUntil,
            quietHoursStart = null,
            quietHoursEnd = null,
            timezone = null,
        )
        assertEquals(listOf("Snoozed until ${expectedSnoozeLabel(snoozedUntil)}"), notificationLines(prefs))
    }

    @Test
    fun `notificationLines omits snooze line when snoozedUntil is in the past`() {
        val prefs = NotificationPreferences(
            notificationsEnabled = true,
            snoozedUntil = Instant.now().minus(30, ChronoUnit.DAYS),
            quietHoursStart = null,
            quietHoursEnd = null,
            timezone = null,
        )
        assertEquals(listOf("Notifications on"), notificationLines(prefs))
    }

    @Test
    fun `notificationLines shows quiet hours with friendly timezone name`() {
        val prefs = NotificationPreferences(
            notificationsEnabled = true,
            snoozedUntil = null,
            quietHoursStart = LocalTime.of(23, 0),
            quietHoursEnd = LocalTime.of(7, 0),
            timezone = "America/New_York",
        )
        assertEquals(listOf("Quiet 11:00 PM–7:00 AM Eastern Time (US)"), notificationLines(prefs))
    }

    @Test
    fun `notificationLines falls back to raw zone id when timezone not in curated list`() {
        val prefs = NotificationPreferences(
            notificationsEnabled = true,
            snoozedUntil = null,
            quietHoursStart = LocalTime.of(23, 0),
            quietHoursEnd = LocalTime.of(7, 0),
            timezone = "Europe/Berlin",
        )
        assertEquals(listOf("Quiet 11:00 PM–7:00 AM Europe/Berlin"), notificationLines(prefs))
    }

    @Test
    fun `notificationLines shows both snooze and quiet hours lines together`() {
        val snoozedUntil = Instant.now().plus(30, ChronoUnit.DAYS)
        val prefs = NotificationPreferences(
            notificationsEnabled = true,
            snoozedUntil = snoozedUntil,
            quietHoursStart = LocalTime.of(23, 0),
            quietHoursEnd = LocalTime.of(7, 0),
            timezone = "America/New_York",
        )
        assertEquals(
            listOf("Snoozed until ${expectedSnoozeLabel(snoozedUntil)}", "Quiet 11:00 PM–7:00 AM Eastern Time (US)"),
            notificationLines(prefs),
        )
    }
}
