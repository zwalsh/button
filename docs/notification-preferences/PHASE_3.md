# Phase 3 — Quiet Hours

Depends on Phase 2. Suppresses notifications during a nightly time window in the contact's local timezone.

---

## PR 3a — Migration, Model, DAO

### Migration: `17_add_quiet_hours.json`

Add three columns and a CHECK constraint to `contact`:

```sql
ALTER TABLE contact
    ADD COLUMN quiet_hours_start TIME,
    ADD COLUMN quiet_hours_end   TIME,
    ADD COLUMN timezone          VARCHAR(64);

ALTER TABLE contact ADD CONSTRAINT contact_quiet_hours_check CHECK (
    (quiet_hours_start IS NULL AND quiet_hours_end IS NULL) OR
    (quiet_hours_start IS NOT NULL AND quiet_hours_end IS NOT NULL AND timezone IS NOT NULL)
);
```

`timezone` alone (without quiet hours) is allowed, so a contact can persist a timezone preference
before setting a quiet window.

### Update `NotificationPreferences`

```kotlin
data class NotificationPreferences(
    val notificationsEnabled: Boolean,
    val snoozedUntil: Instant?,
    val quietHoursStart: LocalTime?,
    val quietHoursEnd: LocalTime?,
    val timezone: String?,
)
```

### Update `ContactDAO.updateNotificationPreferences`

```kotlin
@SqlUpdate("""
    UPDATE contact SET
        notifications_enabled = :notificationsEnabled,
        snoozed_until         = :snoozedUntil,
        quiet_hours_start     = :quietHoursStart,
        quiet_hours_end       = :quietHoursEnd,
        timezone              = :timezone
    WHERE id = :contactId
    RETURNING *
""")
fun updateNotificationPreferences(
    @Bind("contactId") contactId: Int,
    @Bind("notificationsEnabled") notificationsEnabled: Boolean,
    @Bind("snoozedUntil") snoozedUntil: Instant?,
    @Bind("quietHoursStart") quietHoursStart: LocalTime?,
    @Bind("quietHoursEnd") quietHoursEnd: LocalTime?,
    @Bind("timezone") timezone: String?,
): Contact?
```

### Tests

- Integration test: set quiet hours, reload, assert round-trip.
- Assert CHECK constraint rejects `quiet_hours_start` set without `timezone`.
- Assert `timezone` alone (no quiet hours) is accepted.

---

## PR 3b — Filtering, Endpoint, UI

### `ContactNotifier` — `isInQuietHours()`

Add private helper and filter:

```kotlin
.filter { c -> !isInQuietHours(c.notificationPreferences, now) }

private fun isInQuietHours(prefs: NotificationPreferences, now: Instant): Boolean {
    val tz = prefs.timezone ?: return false
    val start = prefs.quietHoursStart ?: return false
    val end = prefs.quietHoursEnd ?: return false
    val localTime = now.atZone(ZoneId.of(tz)).toLocalTime()
    return if (start <= end) {
        localTime >= start && localTime < end
    } else {
        // Wraps midnight, e.g. 23:00–07:00
        localTime >= start || localTime < end
    }
}
```

Log at DEBUG when skipped: `"Skipping contact ${c.id}: in quiet hours (${prefs.quietHoursStart}–${prefs.quietHoursEnd} ${prefs.timezone})"`.

### `POST /contact/preferences` — extend handler

Add quiet hours fields:

| Form field | Notes |
|---|---|
| `quietHoursStart` | `"HH:mm"` or empty — empty clears both |
| `quietHoursEnd` | `"HH:mm"` or empty — empty clears both |
| `timezone` | IANA zone string or empty |

Validation:
- If either `quietHoursStart` or `quietHoursEnd` is non-empty, both must be present and `timezone` must
  be a valid IANA zone ID. Catch `ZoneRulesException` from `ZoneId.of()`, return `400` on failure.
- If both time fields are empty, pass `null` for both (clears quiet hours; `timezone` may still be saved).

### Contact page UI — quiet hours section

Add inside the existing preferences `<form>`, below the snooze section, visible only when `notificationsEnabled`.

Two `<input type="time">` fields and a `<select>` for timezone, on one row:

```
Don't text me between [23:00] and [07:00]   [Eastern Time (US) ▼]
```

Field names: `quietHoursStart`, `quietHoursEnd`, `timezone`.

**Timezone selector**: curated ~35 options. US zones first, then a separator, then others alphabetically
by region. Use friendly display names, with IANA values as the option `value`:

```
Eastern Time (US)    America/New_York
Central Time (US)    America/Chicago
Mountain Time (US)   America/Denver
Pacific Time (US)    America/Los_Angeles
──────────────────
Alaska               America/Anchorage
Hawaii               Pacific/Honolulu
Atlantic (Canada)    America/Halifax
London / UTC         Europe/London
Central Europe       Europe/Paris
Eastern Europe       Europe/Helsinki
Moscow               Europe/Moscow
Dubai                Asia/Dubai
India                Asia/Kolkata
Bangkok              Asia/Bangkok
Singapore / HK       Asia/Singapore
Tokyo                Asia/Tokyo
Sydney               Australia/Sydney
Auckland             Pacific/Auckland
... (add others as needed)
```

Pre-select the saved `timezone` when set. When not yet set, pre-select via browser detection with an
inline script (run after DOM load):

```javascript
const sel = document.getElementById('timezone-select');
if (!sel.value) {
    const tz = Intl.DateTimeFormat().resolvedOptions().timeZone;
    const opt = sel.querySelector(`option[value="${CSS.escape(tz)}"]`);
    if (opt) sel.value = tz;
}
```

### Tests

Unit tests for `isInQuietHours`:
- Standard window (e.g. 22:00–06:00): time inside, outside, at boundary
- Midnight-wrapping window (e.g. 23:00–07:00): same
- Null timezone / null start returns false

Integration test for quiet hours round-trip via `POST /contact/preferences`.

Manual smoke test: set quiet hours spanning current time, verify contact is skipped in `contactsToNotify()`.
