package sh.zachwal.button.admin.contact

import org.junit.jupiter.api.Test
import sh.zachwal.button.db.jdbi.NotificationPreferences
import java.time.Instant
import java.time.LocalTime
import kotlin.test.assertEquals

internal class AdminContactControllerTest {

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
        val prefs = NotificationPreferences(
            notificationsEnabled = true,
            snoozedUntil = Instant.parse("2026-08-16T12:00:00Z"),
            quietHoursStart = null,
            quietHoursEnd = null,
            timezone = null,
        )
        assertEquals(listOf("Snoozed until Aug 16"), notificationLines(prefs))
    }

    @Test
    fun `notificationLines omits snooze line when snoozedUntil is in the past`() {
        val prefs = NotificationPreferences(
            notificationsEnabled = true,
            snoozedUntil = Instant.parse("2020-01-01T00:00:00Z"),
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
        val prefs = NotificationPreferences(
            notificationsEnabled = true,
            snoozedUntil = Instant.parse("2026-08-16T12:00:00Z"),
            quietHoursStart = LocalTime.of(23, 0),
            quietHoursEnd = LocalTime.of(7, 0),
            timezone = "America/New_York",
        )
        assertEquals(
            listOf("Snoozed until Aug 16", "Quiet 11:00 PM–7:00 AM Eastern Time (US)"),
            notificationLines(prefs),
        )
    }
}
