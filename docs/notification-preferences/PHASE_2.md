# Phase 2 — Snooze

Depends on Phase 1. Adds the ability to silence notifications for a fixed duration.

---

## PR 2a — Migration, Model, DAO

### Migration: `16_add_snoozed_until.json`

Add one column to `contact`:

```sql
ALTER TABLE contact
    ADD COLUMN snoozed_until TIMESTAMPTZ;
```

### Update `NotificationPreferences`

```kotlin
data class NotificationPreferences(
    val notificationsEnabled: Boolean,
    val snoozedUntil: Instant?,
)
```

### Update `ContactDAO.updateNotificationPreferences`

Extend the existing method to include the new field:

```kotlin
@SqlUpdate(
    """
    UPDATE contact SET
        notifications_enabled = :notificationsEnabled,
        snoozed_until = :snoozedUntil
    WHERE id = :contactId
    RETURNING *
"""
)
fun updateNotificationPreferences(
    @Bind("contactId") contactId: Int,
    @Bind("notificationsEnabled") notificationsEnabled: Boolean,
    @Bind("snoozedUntil") snoozedUntil: Instant?,
): Contact?
```

### Tests

- Integration test: set `snoozedUntil` to a future instant, reload contact, assert value persists.
- Set `snoozedUntil` to null, assert it clears.

---

## PR 2b — Filtering, Endpoint, UI

### `ContactNotifier.contactsToNotify()`

Add snooze filter alongside the existing `notificationsEnabled` filter:

```kotlin
.filter { c -> c.notificationPreferences.snoozedUntil?.isAfter(now) != true }
```

Log at DEBUG when skipped: `"Skipping contact ${c.id}: snoozed until ${prefs.snoozedUntil}"`.

### `POST /contact/preferences` — extend handler

Add one new field to the form handler:

| Form field     | Type                                   | Notes                          |
|----------------|----------------------------------------|--------------------------------|
| `snoozePreset` | `"none"`, `"1"`, `"7"`, `"30"`, `"90"` | Days from now; `"none"` clears |

Parse `snoozePreset`: if not `"none"`, compute `Instant.now().plus(days.toLong(), ChronoUnit.DAYS)`.
Pass result as `snoozedUntil` to `updateNotificationPreferences`.

### Contact page UI — snooze section

Add inside the existing preferences `<form>`, below the toggle, visible only when `notificationsEnabled`.

A Bootstrap radio button group (`btn-check` + `btn-outline-secondary`):

```
Snooze for:  [None]  [1 day]  [7 days]  [30 days]  [90 days]
```

- `name="snoozePreset"`, values `"none"`, `"1"`, `"7"`, `"30"`, `"90"`
- Pre-select `"none"` by default
- If `snoozedUntil` is non-null and in the future, show a note below the group:
  `"Snoozed until {formatted date}"` (format as e.g. `"Jun 7"` or `"Jun 7, 2026"` if different year)

### Tests

- `snoozePreset = "7"` → `snoozedUntil` is approximately 7 days in the future (within a few seconds)
- `snoozePreset = "none"` → `snoozedUntil` is null
- Manual smoke test: snooze for 1 day, verify it appears in the form and filtering skips the contact
