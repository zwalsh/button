package sh.zachwal.button.admin

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import sh.zachwal.button.db.dao.ContactDAO
import sh.zachwal.button.db.dao.ContactPressCountDAO
import sh.zachwal.button.db.dao.PressDAO
import sh.zachwal.button.db.jdbi.Contact
import sh.zachwal.button.db.jdbi.NotificationPreferences
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ContactPressStatsServiceTest {

    private val contactDAO = mockk<ContactDAO>()
    private val contactPressCountDAO = mockk<ContactPressCountDAO>()
    private val pressDAO = mockk<PressDAO>()

    private val service = ContactPressStatsService(contactDAO, contactPressCountDAO, pressDAO)

    private val today = LocalDate.now(ZoneOffset.UTC)
    private val yesterday = today.minusDays(1)
    private val todayMidnight = today.atStartOfDay(ZoneOffset.UTC).toInstant()

    private fun contact(id: Int, name: String) = Contact(
        id = id,
        createdDate = Instant.EPOCH,
        name = name,
        phoneNumber = "+15550000000",
        active = true,
        notificationPreferences = NotificationPreferences(notificationsEnabled = true),
    )

    // --- TODAY range ---

    @Test
    fun `pressStats TODAY skips materialized query entirely`() {
        every { contactDAO.selectContacts() } returns emptyList()
        every { pressDAO.countByContactSince(any()) } returns emptyMap()

        service.pressStats(TimeRange.TODAY)

        verify(exactly = 0) { contactPressCountDAO.aggregateCountsByContact(any(), any()) }
    }

    @Test
    fun `pressStats TODAY passes today midnight UTC to countByContactSince`() {
        val sinceSlot = slot<Instant>()
        every { contactDAO.selectContacts() } returns emptyList()
        every { pressDAO.countByContactSince(capture(sinceSlot)) } returns emptyMap()

        service.pressStats(TimeRange.TODAY)

        assertThat(sinceSlot.captured).isEqualTo(todayMidnight)
    }

    // --- Materialized query date ranges ---

    @Test
    fun `pressStats LAST_7_DAYS queries materialized from 7 days ago through yesterday`() {
        val startSlot = slot<LocalDate>()
        val endSlot = slot<LocalDate>()
        every { contactDAO.selectContacts() } returns emptyList()
        every { contactPressCountDAO.aggregateCountsByContact(capture(startSlot), capture(endSlot)) } returns emptyMap()
        every { pressDAO.countByContactSince(any()) } returns emptyMap()

        service.pressStats(TimeRange.LAST_7_DAYS)

        assertThat(startSlot.captured).isEqualTo(today.minusDays(7))
        assertThat(endSlot.captured).isEqualTo(yesterday)
    }

    @Test
    fun `pressStats LAST_30_DAYS queries materialized from 30 days ago through yesterday`() {
        val startSlot = slot<LocalDate>()
        val endSlot = slot<LocalDate>()
        every { contactDAO.selectContacts() } returns emptyList()
        every { contactPressCountDAO.aggregateCountsByContact(capture(startSlot), capture(endSlot)) } returns emptyMap()
        every { pressDAO.countByContactSince(any()) } returns emptyMap()

        service.pressStats(TimeRange.LAST_30_DAYS)

        assertThat(startSlot.captured).isEqualTo(today.minusDays(30))
        assertThat(endSlot.captured).isEqualTo(yesterday)
    }

    @Test
    fun `pressStats YEAR_TO_DATE queries materialized from Jan 1 of current year through yesterday`() {
        val startSlot = slot<LocalDate>()
        val endSlot = slot<LocalDate>()
        every { contactDAO.selectContacts() } returns emptyList()
        every { contactPressCountDAO.aggregateCountsByContact(capture(startSlot), capture(endSlot)) } returns emptyMap()
        every { pressDAO.countByContactSince(any()) } returns emptyMap()

        service.pressStats(TimeRange.YEAR_TO_DATE)

        assertThat(startSlot.captured).isEqualTo(LocalDate.of(today.year, 1, 1))
        assertThat(endSlot.captured).isEqualTo(yesterday)
    }

    @Test
    fun `pressStats ALL_TIME queries materialized from epoch day zero through yesterday`() {
        val startSlot = slot<LocalDate>()
        val endSlot = slot<LocalDate>()
        every { contactDAO.selectContacts() } returns emptyList()
        every { contactPressCountDAO.aggregateCountsByContact(capture(startSlot), capture(endSlot)) } returns emptyMap()
        every { pressDAO.countByContactSince(any()) } returns emptyMap()

        service.pressStats(TimeRange.ALL_TIME)

        assertThat(startSlot.captured).isEqualTo(LocalDate.ofEpochDay(0))
        assertThat(endSlot.captured).isEqualTo(yesterday)
    }

    @Test
    fun `pressStats non-TODAY ranges always pass today midnight UTC to countByContactSince`() {
        val sinceSlot = slot<Instant>()
        every { contactDAO.selectContacts() } returns emptyList()
        every { contactPressCountDAO.aggregateCountsByContact(any(), any()) } returns emptyMap()
        every { pressDAO.countByContactSince(capture(sinceSlot)) } returns emptyMap()

        service.pressStats(TimeRange.LAST_7_DAYS)

        assertThat(sinceSlot.captured).isEqualTo(todayMidnight)
    }

    // --- Merge logic ---

    @Test
    fun `pressStats sums materialized and today counts for the same contact`() {
        val alice = contact(1, "Alice")
        every { contactDAO.selectContacts() } returns listOf(alice)
        every { contactPressCountDAO.aggregateCountsByContact(any(), any()) } returns mapOf(1 to 10)
        every { pressDAO.countByContactSince(any()) } returns mapOf(1 to 3L)

        val result = service.pressStats(TimeRange.LAST_7_DAYS)

        assertThat(result).hasSize(1)
        assertThat(result[0].count).isEqualTo(13L)
    }

    @Test
    fun `pressStats contact with only materialized count appears with correct total`() {
        val alice = contact(1, "Alice")
        every { contactDAO.selectContacts() } returns listOf(alice)
        every { contactPressCountDAO.aggregateCountsByContact(any(), any()) } returns mapOf(1 to 7)
        every { pressDAO.countByContactSince(any()) } returns emptyMap()

        val result = service.pressStats(TimeRange.LAST_30_DAYS)

        assertThat(result).hasSize(1)
        assertThat(result[0].count).isEqualTo(7L)
    }

    @Test
    fun `pressStats contact with only today count appears with correct total`() {
        val alice = contact(1, "Alice")
        every { contactDAO.selectContacts() } returns listOf(alice)
        every { contactPressCountDAO.aggregateCountsByContact(any(), any()) } returns emptyMap()
        every { pressDAO.countByContactSince(any()) } returns mapOf(1 to 4L)

        val result = service.pressStats(TimeRange.LAST_30_DAYS)

        assertThat(result).hasSize(1)
        assertThat(result[0].count).isEqualTo(4L)
    }

    @Test
    fun `pressStats multiple contacts with overlapping data merge correctly`() {
        val alice = contact(1, "Alice")
        val bob = contact(2, "Bob")
        every { contactDAO.selectContacts() } returns listOf(alice, bob)
        every { contactPressCountDAO.aggregateCountsByContact(any(), any()) } returns mapOf(1 to 10, 2 to 3)
        every { pressDAO.countByContactSince(any()) } returns mapOf(1 to 2L)

        val result = service.pressStats(TimeRange.LAST_7_DAYS)

        val byId = result.associateBy { it.contact.id }
        assertThat(byId[1]!!.count).isEqualTo(12L)
        assertThat(byId[2]!!.count).isEqualTo(3L)
    }

    // --- Filtering ---

    @Test
    fun `pressStats excludes contacts with zero total count`() {
        val alice = contact(1, "Alice")
        val bob = contact(2, "Bob")
        every { contactDAO.selectContacts() } returns listOf(alice, bob)
        every { contactPressCountDAO.aggregateCountsByContact(any(), any()) } returns mapOf(1 to 5)
        every { pressDAO.countByContactSince(any()) } returns emptyMap()

        val result = service.pressStats(TimeRange.LAST_30_DAYS)

        assertThat(result).hasSize(1)
        assertThat(result[0].contact).isEqualTo(alice)
    }

    @Test
    fun `pressStats excludes contact IDs in press data that are absent from contactDAO`() {
        val alice = contact(1, "Alice")
        every { contactDAO.selectContacts() } returns listOf(alice)
        // Contact ID 99 appears in press data but not in contactDAO (FK mismatch)
        every { contactPressCountDAO.aggregateCountsByContact(any(), any()) } returns mapOf(1 to 3, 99 to 10)
        every { pressDAO.countByContactSince(any()) } returns emptyMap()

        val result = service.pressStats(TimeRange.LAST_30_DAYS)

        assertThat(result).hasSize(1)
        assertThat(result[0].contact).isEqualTo(alice)
    }

    @Test
    fun `pressStats returns empty list when no presses in range`() {
        every { contactDAO.selectContacts() } returns listOf(contact(1, "Alice"))
        every { contactPressCountDAO.aggregateCountsByContact(any(), any()) } returns emptyMap()
        every { pressDAO.countByContactSince(any()) } returns emptyMap()

        val result = service.pressStats(TimeRange.LAST_30_DAYS)

        assertThat(result).isEmpty()
    }

    // --- Sort order ---

    @Test
    fun `pressStats result is sorted by count descending`() {
        val alice = contact(1, "Alice")
        val bob = contact(2, "Bob")
        val carol = contact(3, "Carol")
        every { contactDAO.selectContacts() } returns listOf(alice, bob, carol)
        every { contactPressCountDAO.aggregateCountsByContact(any(), any()) } returns mapOf(1 to 2, 2 to 10, 3 to 5)
        every { pressDAO.countByContactSince(any()) } returns emptyMap()

        val result = service.pressStats(TimeRange.LAST_30_DAYS)

        assertThat(result.map { it.contact }).containsExactly(bob, carol, alice).inOrder()
        assertThat(result.map { it.count }).containsExactly(10L, 5L, 2L).inOrder()
    }

    // --- allContactStats ---

    @Test
    fun `allContactStats includes contacts with zero presses`() {
        val alice = contact(1, "Alice")
        val bob = contact(2, "Bob")
        every { contactDAO.selectContacts() } returns listOf(alice, bob)
        every { contactPressCountDAO.aggregateCountsByContact(any(), any()) } returns mapOf(1 to 5)
        every { pressDAO.countByContactSince(any()) } returns emptyMap()

        val result = service.allContactStats(TimeRange.LAST_90_DAYS)

        assertThat(result.map { it.contact }).containsExactly(alice, bob)
    }

    @Test
    fun `allContactStats sorts by press count descending with zero-press contacts at the end`() {
        val alice = contact(1, "Alice")
        val bob = contact(2, "Bob")
        val carol = contact(3, "Carol")
        every { contactDAO.selectContacts() } returns listOf(alice, bob, carol)
        every { contactPressCountDAO.aggregateCountsByContact(any(), any()) } returns mapOf(3 to 10, 1 to 3)
        every { pressDAO.countByContactSince(any()) } returns emptyMap()

        val result = service.allContactStats(TimeRange.LAST_90_DAYS)

        assertThat(result.map { it.contact }).containsExactly(carol, alice, bob).inOrder()
        assertThat(result.map { it.count }).containsExactly(10L, 3L, 0L).inOrder()
    }

    @Test
    fun `allContactStats returns correct counts for contacts with presses`() {
        val alice = contact(1, "Alice")
        every { contactDAO.selectContacts() } returns listOf(alice)
        every { contactPressCountDAO.aggregateCountsByContact(any(), any()) } returns mapOf(1 to 7)
        every { pressDAO.countByContactSince(any()) } returns mapOf(1 to 3L)

        val result = service.allContactStats(TimeRange.LAST_90_DAYS)

        assertThat(result).hasSize(1)
        assertThat(result[0].count).isEqualTo(10L)
    }

    // --- TimeRange.fromParam ---

    @Test
    fun `pressStats LAST_90_DAYS queries materialized from 90 days ago through yesterday`() {
        val startSlot = slot<LocalDate>()
        val endSlot = slot<LocalDate>()
        every { contactDAO.selectContacts() } returns emptyList()
        every { contactPressCountDAO.aggregateCountsByContact(capture(startSlot), capture(endSlot)) } returns emptyMap()
        every { pressDAO.countByContactSince(any()) } returns emptyMap()

        service.pressStats(TimeRange.LAST_90_DAYS)

        assertThat(startSlot.captured).isEqualTo(today.minusDays(90))
        assertThat(endSlot.captured).isEqualTo(yesterday)
    }

    @Test
    fun `TimeRange fromParam falls back to LAST_30_DAYS for null, empty, and unknown params`() {
        assertThat(TimeRange.fromParam(null)).isEqualTo(TimeRange.LAST_30_DAYS)
        assertThat(TimeRange.fromParam("")).isEqualTo(TimeRange.LAST_30_DAYS)
        assertThat(TimeRange.fromParam("bogus")).isEqualTo(TimeRange.LAST_30_DAYS)
    }

    @Test
    fun `TimeRange fromParam resolves all known query params`() {
        assertThat(TimeRange.fromParam("today")).isEqualTo(TimeRange.TODAY)
        assertThat(TimeRange.fromParam("7d")).isEqualTo(TimeRange.LAST_7_DAYS)
        assertThat(TimeRange.fromParam("30d")).isEqualTo(TimeRange.LAST_30_DAYS)
        assertThat(TimeRange.fromParam("90d")).isEqualTo(TimeRange.LAST_90_DAYS)
        assertThat(TimeRange.fromParam("ytd")).isEqualTo(TimeRange.YEAR_TO_DATE)
        assertThat(TimeRange.fromParam("all")).isEqualTo(TimeRange.ALL_TIME)
    }
}
