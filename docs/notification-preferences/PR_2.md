# PR 2 — Backend Filtering + Preferences Endpoint

Depends on PR 1. Wires up the preference data to notification suppression and exposes the update endpoint.
No contact-visible UI yet — the endpoint will be used by the form added in PR 3.

## `ContactNotifier` — filter by preferences

In `contactsToNotify()`, apply three filters after `contactDAO.selectActiveContacts()` and before the
press-count sort:

```kotlin
private fun contactsToNotify(): List<Contact> {
    val now = Instant.now()
    val contacts = contactDAO.selectActiveContacts()
        .filter { it.notificationPreferences.notificationsEnabled }
        .filter { c -> c.notificationPreferences.snoozedUntil?.isAfter(now) != true }
        .filter { c -> !isInQuietHours(c.notificationPreferences, now) }
    val endDate = LocalDate.now()
    val startDate = endDate.minusDays(90)
    val aggregatedCounts = contactPressCountDAO.aggregateCountsByContact(startDate, endDate)
    return contacts.sortedByDescending { c -> aggregatedCounts[c.id] ?: 0 }
}

private fun isInQuietHours(prefs: NotificationPreferences, now: Instant): Boolean {
    val tz = prefs.timezone ?: return false
    val start = prefs.quietHoursStart ?: return false
    val end = prefs.quietHoursEnd ?: return false
    val localTime = now.atZone(ZoneId.of(tz)).toLocalTime()
    return if (start <= end) {
        localTime >= start && localTime < end
    } else {
        // Wraps midnight: e.g. 23:00–07:00
        localTime >= start || localTime < end
    }
}
```

Add a log line when a contact is skipped (at DEBUG level), e.g.:
`"Skipping contact ${c.id}: notificationsEnabled=false"` / `"snoozed until ${prefs.snoozedUntil}"` / `"in quiet hours"`.

## `POST /contact/preferences` route

Add to `ContactController`. Receives a standard HTML form POST (not JSON). Field names:

| Form field | Type | Notes |
|---|---|---|
| `notificationsEnabled` | checkbox (`"on"` / absent) | |
| `snoozePreset` | `"none"`, `"1"`, `"7"`, `"30"`, `"90"` | Days to snooze from now; `"none"` clears |
| `quietHoursStart` | `"HH:mm"` string or empty | |
| `quietHoursEnd` | `"HH:mm"` string or empty | |
| `timezone` | IANA zone string or empty | |

Handler logic:

1. Parse `notificationsEnabled` (checkbox: present = true, absent = false).
2. Parse `snoozePreset`: if not `"none"`, compute `Instant.now().plus(days, ChronoUnit.DAYS)`.
3. Parse quiet hours: if `quietHoursStart` and `quietHoursEnd` are both non-empty, parse as `LocalTime`.
   If either is empty, clear both (set to null).
4. Validate: if quiet hours are set, `timezone` must be a valid IANA zone (`ZoneId.of()` catching
   `ZoneRulesException`). Return `400` on validation failure.
5. Call `contactDAO.updateNotificationPreferences(...)`.
6. Redirect to `GET /contact?saved=true`.

## Tests

Unit tests for `isInQuietHours`:
- Standard window (e.g. 22:00–06:00): test a time inside, outside, and at boundary
- Midnight-wrapping window (e.g. 23:00–07:00): test same
- Null timezone / null start returns false

Integration test for `POST /contact/preferences`:
- Happy path: all fields round-trip correctly
- Checkbox absent → `notificationsEnabled = false`
- `snoozePreset = "7"` → `snoozedUntil` is ~7 days in the future
- Invalid timezone → 400
- Quiet hours without timezone → 400
- Successful post redirects to `/contact?saved=true`
