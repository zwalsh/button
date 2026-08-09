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

**Superseded in PR 2b:** the combined `updateNotificationPreferences(contactId, notificationsEnabled,
snoozedUntil)` method was split into `updateNotificationsEnabled(contactId, enabled)` and
`updateSnoozedUntil(contactId, snoozedUntil)` — see the PR 2b UI note below for why.

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

**As shipped, this whole section was redesigned** after an early draft surfaced a bug: a single form
covering both the toggle and the snooze radios meant re-submitting for any reason (e.g. flipping the
toggle) also resubmitted whatever the snooze radios happened to be showing — which defaulted to `"none"`
— silently clearing an active snooze the user never touched. The fix was to drop the single "Save" form
entirely in favor of one independent one-click form per action, all still inside a single "Notification
Settings" card:

- **Toggle** — no longer a checkbox/switch; a `[Turn Off]` / `[Turn On]` button, each its own
  `POST /contact/preferences/notifications` form with a hidden `notificationsEnabled` field carrying the
  *target* state. Clicking submits immediately — no separate Save.
- **Snooze, not currently snoozed** — one button per preset (`[1 day] [7 days] [30 days] [90 days]`), each
  its own `POST /contact/preferences/snooze` form with a hidden `days` field. Clicking immediately snoozes
  and the page reloads showing the "Snoozed until" state below.
- **Snooze, currently snoozed** — replaces the preset buttons with `"Snoozed until {formatted date}"` and a
  `[Clear Snooze]` button, its own `POST /contact/preferences/snooze` form with no `days` field (parses to
  `null`, clearing the snooze).

Each form only ever carries the field(s) for the action it represents, so no action can accidentally
overwrite unrelated state — the DAO was split accordingly (see the PR 2a note above). This required no
client-side JS: every "button" is a real form submit, consistent with the rest of the page.

This app vendors Bootstrap 4.4.1 (see `sharedhtml/Head.kt`), which has no `btn-check` component (a
Bootstrap 5 addition) — buttons use plain `btn btn-outline-secondary`. All rendering lives in
`notificationSettingsCard()` / `snoozeSection()` in `ContactController.kt`; the snooze section is only
rendered at all when `notificationsEnabled` is currently true, same as before. Date formatting is
`formatSnoozedUntil()`, using `America/New_York` (matching the zone convention in
`AdminStatsController.kt`) — timezone-aware quiet-hours display is Phase 3's concern, not this one's.

### Tests

- `snoozePreset = "7"` → `snoozedUntil` is approximately 7 days in the future (within a few seconds)
- `snoozePreset = "none"` → `snoozedUntil` is null
- Manual smoke test: snooze for 1 day, verify it appears in the form and filtering skips the contact

**As shipped:** `ContactControllerTest` covers `POST /contact/preferences/notifications` with
`notificationsEnabled=true`/`false`, and `POST /contact/preferences/snooze` with `days=7` and with no
`days` field (clear), plus two `formatSnoozedUntil` unit tests (same-year and cross-year formatting).
`ContactDAOTest` covers `updateNotificationsEnabled` and `updateSnoozedUntil` independently, including
that each leaves the other field untouched. `ContactNotifierTest` covers a snoozed contact being skipped
and a contact whose snooze has already expired being notified normally.
