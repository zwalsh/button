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

## PR 3b — Filtering

**Scope note:** the original plan bundled filtering, endpoint, and UI into one PR. That was split at
implementation time: this PR ships filtering only; endpoint + UI move to [PR 3c](#pr-3c--endpoint-ui) below
(not yet started). Until 3c ships, quiet hours can only be set directly in the database — there's no
user-facing way to configure them yet, so the filter is effectively inert in production but fully tested.

### `ContactNotifier` — `isInQuietHours()` — shipped

Added as an `internal` (not `private`) helper so it's directly unit-testable, and a filter clause:

```kotlin
.filter { c -> !isInQuietHours(c.notificationPreferences, now) }

internal fun isInQuietHours(prefs: NotificationPreferences, now: Instant): Boolean {
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

Logs at INFO when skipped, matching the existing disabled/snoozed filters' format (not DEBUG as originally
planned):
`"Skipping contact id=${c.id} name=${c.name}: in quiet hours (${prefs.quietHoursStart}–${prefs.quietHoursEnd} ${prefs.timezone})"`.

### Tests — shipped

Unit tests for `isInQuietHours` in `ContactNotifierTest.kt`:
- Standard window (22:00–23:00 UTC): time inside, outside, at each boundary
- Midnight-wrapping window (23:00–07:00 UTC): same, both sides of midnight
- Null timezone / null start returns false

Plus one `pressed()`-level integration test confirming a contact whose quiet window contains the current
time is skipped while another contact is still notified.

---

## PR 3c — Endpoint, UI — shipped

Adds the `POST /contact/preferences/quiet-hours` endpoint and the contact-page UI for setting quiet hours.
Two changes from the original plan below, made during manual QA:

- **No browser-detection JS.** `timezone` has had a DB-level default of `America/New_York` since migration
  `17_add_quiet_hours.json`, so `NotificationPreferences.timezone` is never actually `null` in practice — the
  placeholder option and `Intl.DateTimeFormat()` detection script were dead code (the select was never empty
  for the script to fill in). Dropped both; the `<select>` always has the contact's saved zone (or
  `America/New_York`) pre-selected, and the user picks manually if that's wrong.
- **Added a "Clear Quiet Hours" button**, shown only when quiet hours are currently set. It's a second
  `<form>` (same pattern as "Clear Snooze") posting hidden empty `quietHoursStart`/`quietHoursEnd` fields
  plus the currently-saved `timezone`, so clearing the window doesn't also drop the timezone preference.
- **Each option is labeled with its live GMT offset**, e.g. `(GMT+02:00) Central Europe`, computed
  per-request from `ZoneId.rules.getOffset()` so it stays correct across DST transitions (a pattern borrowed
  from `dailygames`' `UserPreferencesService.displayString()`). Considered switching to `dailygames`' full
  `ZoneId.getAvailableZoneIds()` list (604 IDs, sorted by offset) instead of a curated set, but rejected it —
  that list is mostly legacy aliases (`US/Pacific`, `Canada/Eastern`, sign-inverted `Etc/GMT+n`) and would be
  more noise than help for a small contact list, and its GMT-string sort has a bug where UTC (`"Z"`) sorts
  out of place. The non-US options are sorted by that same live offset (ascending, west to east) when the
  `<select>` is rendered, rather than hardcoded in offset order, so the ordering stays correct across DST
  changes too.
- **Checked the curated list for population coverage** rather than assuming it: walked every ~1-hour-wide
  GMT offset band and confirmed at least one zone represents it. Found and fixed two gaps — no zone for
  GMT+01:00 outside DST-only London (added `Africa/Lagos`, ~220M-person Nigeria/West Africa, no DST) and no
  zone at all for GMT+06:00 (added `Asia/Dhaka`, ~170M-person Bangladesh). Also added `Africa/Nairobi`
  (GMT+03:00) so East Africa isn't only represented by European cities at the same offset.

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

Pre-select the saved `timezone` (see as-built note above — always set in practice, so no browser-detection
fallback was needed).

### Tests

Integration test for quiet hours round-trip via `POST /contact/preferences/quiet-hours`.

Manual smoke test: set quiet hours spanning current time, verify contact is skipped in `contactsToNotify()`.
