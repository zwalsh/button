# Phase 2 — Snooze

Depends on Phase 1. Adds the ability to silence notifications for a fixed duration.

**Status:** PR 2a merged as "Phase 2a: snoozed_until column, NotificationPreferences model, ContactDAO" (#123,
9888573). PR 2b (this doc's second section) is in progress.

A few implementation details differ from the plan below; see the "As shipped" notes at the end of each
section.

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

**As shipped:** matches the plan as written. `ContactController`'s existing `POST /contact/preferences`
handler was updated to pass `snoozedUntil = null` through to the DAO for now, since real snooze
parsing/UI lands in PR 2b. Tests live in `ContactDAOTest`, `ContactControllerTest`, and `ContactFixtures`
(the `contact()` test fixture gained a `snoozedUntil` parameter).

---

## PR 2b — Filtering, Endpoint, UI

### `ContactNotifier.contactsToNotify()`

Add snooze filter alongside the existing `notificationsEnabled` filter:

```kotlin
.filter { c -> c.notificationPreferences.snoozedUntil?.isAfter(now) != true }
```

Log at DEBUG when skipped: `"Skipping contact ${c.id}: snoozed until ${prefs.snoozedUntil}"`.

**As shipped:** the snooze check was folded into the existing `notificationPreferences` filter in
`contactsToNotify()` (`ContactNotifier.kt`), alongside the `notificationsEnabled` check, rather than a
separate chained `.filter { ... }`. The skip log is at INFO (matching the Phase 1 precedent), not DEBUG:
`"Skipping contact id=${c.id} name=${c.name}: snoozed until ${prefs.snoozedUntil}"`.

### `POST /contact/preferences` — extend handler

Add one new field to the form handler:

| Form field     | Type                                   | Notes                          |
|----------------|----------------------------------------|--------------------------------|
| `snoozePreset` | `"none"`, `"1"`, `"7"`, `"30"`, `"90"` | Days from now; `"none"` clears |

Parse `snoozePreset`: if not `"none"`, compute `Instant.now().plus(days.toLong(), ChronoUnit.DAYS)`.
Pass result as `snoozedUntil` to `updateNotificationPreferences`.

**As shipped:** parsed as `params["snoozePreset"]?.toLongOrNull()` — both a missing field and the literal
value `"none"` fail to parse as a `Long` and fall through to `snoozedUntil = null`, so no explicit
`"none"` branch was needed.

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

**As shipped:** this app vendors Bootstrap 4.4.1 (see `sharedhtml/Head.kt`), which has no `btn-check`
component (that's a Bootstrap 5 addition). Used the Bootstrap 4 toggle-buttons pattern instead:
`.btn-group.btn-group-toggle[data-toggle="buttons"]` wrapping `<label class="btn btn-outline-secondary">`
elements, each containing a radio `<input>`. The section is rendered by `snoozeSection()` in
`ContactController.kt`, only when `notificationsEnabled` is currently true (matches the existing
static-render pattern used for the opt-out helper text — there's no live JS toggling on the form).
Date formatting is `formatSnoozedUntil()`, using `America/New_York` (matching the zone convention in
`AdminStatsController.kt`) — timezone-aware quiet-hours display is Phase 3's concern, not this one's.

### Tests

- `snoozePreset = "7"` → `snoozedUntil` is approximately 7 days in the future (within a few seconds)
- `snoozePreset = "none"` → `snoozedUntil` is null
- Manual smoke test: snooze for 1 day, verify it appears in the form and filtering skips the contact

**As shipped:** `ContactControllerTest` covers `snoozePreset = "7"` and `snoozePreset = "none"`, plus two
`formatSnoozedUntil` unit tests (same-year and cross-year formatting). `ContactNotifierTest` covers a
snoozed contact being skipped and a contact whose snooze has already expired being notified normally.
