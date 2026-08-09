# Phase 3 — Quiet Hours

Depends on Phase 2. Suppresses notifications during a nightly time window in the contact's local timezone.

**Note on Phase 2b's redesign:** the plan below still describes a single combined `POST /contact/preferences`
endpoint and a single combined `ContactDAO.updateNotificationPreferences` method, matching how this doc
originally read before Phase 2 shipped. Phase 2b replaced that with independent per-action endpoints and
DAO methods (`/contact/preferences/notifications` + `updateNotificationsEnabled`, and
`/contact/preferences/snooze` + `updateSnoozedUntil`) after a combined form/DAO method turned out to let one
action silently clobber another's state (see `PHASE_2.md`'s PR 2b notes). Quiet hours should follow the same
narrow-DAO-method precedent — see the updated PR 3a/3b sections below.

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

### Add `ContactDAO.updateQuietHours`

As of Phase 2b, `ContactDAO` no longer has one combined preferences-update method — it has one narrow
method per independently-editable concern (`updateNotificationsEnabled`, `updateSnoozedUntil`). Quiet hours'
three fields are always edited together (see PR 3b), so they get one more method for that group, still
scoped only to its own columns:

```kotlin
@SqlQuery("""
    UPDATE contact SET
        quiet_hours_start = :quietHoursStart,
        quiet_hours_end   = :quietHoursEnd,
        timezone          = :timezone
    WHERE id = :contactId
    RETURNING *
""")
fun updateQuietHours(
    @Bind("contactId") contactId: Int,
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

### `POST /contact/preferences/quiet-hours` — new endpoint

A dedicated endpoint, not a shared/combined one — following the Phase 2b precedent of one endpoint per
independently-submitted concern. Unlike the toggle and snooze actions (immediate one-click buttons), quiet
hours has three correlated fields that must be submitted together, so this is the one section that keeps a
traditional `<form>` with an explicit Save button.

| Form field | Notes |
|---|---|
| `quietHoursStart` | `"HH:mm"` or empty — empty clears both |
| `quietHoursEnd` | `"HH:mm"` or empty — empty clears both |
| `timezone` | IANA zone string or empty |

Validation:
- If either `quietHoursStart` or `quietHoursEnd` is non-empty, both must be present and `timezone` must
  be a valid IANA zone ID. Catch `ZoneRulesException` from `ZoneId.of()`, return `400` on failure.
- If both time fields are empty, pass `null` for both (clears quiet hours; `timezone` may still be saved).
- Handler calls `contactDAO.updateQuietHours(...)`, not the old combined method.

### Contact page UI — quiet hours section

Add as its own `<form>`+Save button inside the same "Notification Settings" card used by the toggle and
snooze sections (`notificationSettingsCard()` in `ContactController.kt`), separated from the snooze section
above it by an `<hr>` — same pattern Phase 2b used to separate the toggle section from the snooze section.
Visible only when `notificationsEnabled`.

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

Pre-select the saved `timezone` when set. When not yet set, pre-select via browser detection with a script added via 
`frontend/README.md` conventions:

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

Integration test for quiet hours round-trip via `POST /contact/preferences/quiet-hours`.

Manual smoke test: set quiet hours spanning current time, verify contact is skipped in `contactsToNotify()`.
